package com.groovylsp.presentation.server;

import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jspecify.annotations.Nullable;

/** Groovy Language Serverの主要実装。 */
@Singleton
public class GroovyLanguageServer implements LanguageServer, LanguageClientAware {

  private @Nullable LanguageClient client;

  private final GroovyTextDocumentService textDocumentService;
  private final GroovyWorkspaceService workspaceService;

  @Inject
  public GroovyLanguageServer(
      GroovyTextDocumentService textDocumentService, GroovyWorkspaceService workspaceService) {
    this.textDocumentService = textDocumentService;
    this.workspaceService = workspaceService;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    var capabilities = new ServerCapabilities();

    // テキストドキュメント同期機能
    capabilities.setTextDocumentSync(org.eclipse.lsp4j.TextDocumentSyncKind.Full);

    // ドキュメントシンボル機能
    capabilities.setDocumentSymbolProvider(true);

    var result = new InitializeResult(capabilities);
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public void initialized(InitializedParams params) {
    // クライアントがInitializeResultを受信した後、他のリクエスト/通知の前に呼び出される
    // ここで動的機能を登録し、初期化後のセットアップを実行できる
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    // クリーンシャットダウン
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void exit() {
    // サーバープロセスを終了
    System.exit(0);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textDocumentService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return workspaceService;
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
    this.textDocumentService.connect(client);
  }

  public @Nullable LanguageClient getClient() {
    return client;
  }
}
