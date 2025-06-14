package com.groovylsp.presentation.server;

import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jspecify.annotations.Nullable;

/** Main implementation of the Groovy Language Server. */
@Singleton
public class GroovyLanguageServer implements LanguageServer, LanguageClientAware {

  @Nullable private LanguageClient client;

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
    ServerCapabilities capabilities = new ServerCapabilities();
    // For now, we don't advertise any capabilities
    // These will be added incrementally in future milestones

    InitializeResult result = new InitializeResult(capabilities);
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    // Clean shutdown
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void exit() {
    // Exit the server process
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

  @Nullable
  public LanguageClient getClient() {
    return client;
  }
}
