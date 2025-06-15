package com.groovylsp.application.usecase;

import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import io.vavr.control.Either;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TextDocumentSyncUseCase {

  private static final Logger logger = LoggerFactory.getLogger(TextDocumentSyncUseCase.class);

  private final TextDocumentRepository repository;

  @Inject
  public TextDocumentSyncUseCase(TextDocumentRepository repository) {
    this.repository = repository;
  }

  public Either<SyncError, TextDocument> openDocument(DidOpenTextDocumentParams params) {
    var textDocument = params.getTextDocument();
    var uri = URI.create(textDocument.getUri());
    var document =
        new TextDocument(
            uri, textDocument.getLanguageId(), textDocument.getVersion(), textDocument.getText());

    logger.info("Opening document: {} (version: {})", uri, textDocument.getVersion());

    return repository.save(document).mapLeft(err -> new SyncError.RepositoryError(err.toString()));
  }

  public Either<SyncError, TextDocument> changeDocument(DidChangeTextDocumentParams params) {
    var identifier = params.getTextDocument();
    var uri = URI.create(identifier.getUri());
    var version = identifier.getVersion();

    logger.info("Changing document: {} (version: {})", uri, version);

    return repository
        .findByUri(uri)
        .toEither(() -> (SyncError) new SyncError.DocumentNotFound(uri))
        .flatMap(
            document -> {
              var newContent = applyChanges(document.content(), params.getContentChanges());
              var updatedDocument = document.withContent(newContent, version);
              return repository
                  .save(updatedDocument)
                  .mapLeft(err -> (SyncError) new SyncError.RepositoryError(err.toString()));
            });
  }

  public Either<SyncError, URI> closeDocument(DidCloseTextDocumentParams params) {
    var uri = URI.create(params.getTextDocument().getUri());

    logger.info("Closing document: {}", uri);

    return repository
        .remove(uri)
        .map(doc -> uri)
        .mapLeft(err -> (SyncError) new SyncError.RepositoryError(err.toString()));
  }

  private String applyChanges(
      String content, java.util.List<? extends TextDocumentContentChangeEvent> changes) {
    var result = content;
    for (var change : changes) {
      if (change.getRange() == null) {
        // フルドキュメント同期
        result = change.getText();
      } else {
        // TODO: インクリメンタル同期のサポートを実装
        // これには、範囲（行/文字）からオフセットを計算し、
        // 正しい位置にテキスト変更を適用する必要がある。
        // 現在はフルドキュメント同期のみサポート。
        throw new UnsupportedOperationException("インクリメンタル同期はまだサポートされていません");
      }
    }
    return result;
  }

  public sealed interface SyncError {
    record DocumentNotFound(URI uri) implements SyncError {}

    record RepositoryError(String message) implements SyncError {}

    record UnexpectedError(String message) implements SyncError {}
  }
}
