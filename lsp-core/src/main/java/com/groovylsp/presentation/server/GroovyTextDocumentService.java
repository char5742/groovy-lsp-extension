package com.groovylsp.presentation.server;

import com.groovylsp.application.usecase.DiagnosticUseCase;
import com.groovylsp.application.usecase.TextDocumentSyncUseCase;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.DiagnosticResult;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Groovyファイル用のテキストドキュメントサービス実装。 */
@Singleton
public class GroovyTextDocumentService implements TextDocumentService, LanguageClientAware {

  private static final Logger logger = LoggerFactory.getLogger(GroovyTextDocumentService.class);

  private @Nullable LanguageClient client;
  private final TextDocumentSyncUseCase syncUseCase;
  private final DiagnosticUseCase diagnosticUseCase;

  @Inject
  public GroovyTextDocumentService(
      TextDocumentSyncUseCase syncUseCase, DiagnosticUseCase diagnosticUseCase) {
    this.syncUseCase = syncUseCase;
    this.diagnosticUseCase = diagnosticUseCase;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    syncUseCase
        .openDocument(params)
        .peek(
            document -> {
              logger.info("Successfully opened document: {}", document.uri());
              runDiagnostics(document);
            })
        .peekLeft(error -> logger.error("Failed to open document: {}", error));
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    syncUseCase
        .changeDocument(params)
        .peek(
            document -> {
              logger.info(
                  "Successfully changed document: {} (version: {})",
                  document.uri(),
                  document.version());
              runDiagnostics(document);
            })
        .peekLeft(error -> logger.error("Failed to change document: {}", error));
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    syncUseCase
        .closeDocument(params)
        .peek(uri -> logger.info("Successfully closed document: {}", uri))
        .peekLeft(error -> logger.error("Failed to close document: {}", error));
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    logger.info("Document saved: {}", params.getTextDocument().getUri());
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  protected @Nullable LanguageClient getClient() {
    return client;
  }

  /**
   * ドキュメントの診断を実行し、結果をクライアントに送信する。
   *
   * @param document 診断対象のドキュメント
   */
  private void runDiagnostics(com.groovylsp.domain.model.TextDocument document) {
    var currentClient = client;
    if (currentClient == null) {
      logger.warn("Language client not connected, skipping diagnostics");
      return;
    }

    diagnosticUseCase
        .diagnose(document)
        .peek(
            result -> {
              var diagnostics = convertToLspDiagnostics(result);
              var params = new PublishDiagnosticsParams(document.uri().toString(), diagnostics);
              currentClient.publishDiagnostics(params);
              logger.info("Published {} diagnostics for {}", diagnostics.size(), document.uri());
            })
        .peekLeft(error -> logger.error("Failed to run diagnostics: {}", error));
  }

  /**
   * ドメインモデルの診断結果をLSPのDiagnosticに変換する。
   *
   * @param result 診断結果
   * @return LSPのDiagnosticリスト
   */
  private List<Diagnostic> convertToLspDiagnostics(DiagnosticResult result) {
    var diagnostics = new ArrayList<Diagnostic>();
    for (var item : result.diagnostics()) {
      var diagnostic = new Diagnostic();
      diagnostic.setRange(
          new Range(
              new Position(item.startPosition().line(), item.startPosition().character()),
              new Position(item.endPosition().line(), item.endPosition().character())));
      diagnostic.setSeverity(convertSeverity(item.severity()));
      diagnostic.setMessage(item.message());
      diagnostic.setSource(item.source());
      diagnostics.add(diagnostic);
    }
    return diagnostics;
  }

  /**
   * 診断の重要度をLSPのDiagnosticSeverityに変換する。
   *
   * @param severity ドメインモデルの重要度
   * @return LSPのDiagnosticSeverity
   */
  private DiagnosticSeverity convertSeverity(DiagnosticItem.DiagnosticSeverity severity) {
    return switch (severity) {
      case ERROR -> DiagnosticSeverity.Error;
      case WARNING -> DiagnosticSeverity.Warning;
      case INFORMATION -> DiagnosticSeverity.Information;
      case HINT -> DiagnosticSeverity.Hint;
    };
  }
}
