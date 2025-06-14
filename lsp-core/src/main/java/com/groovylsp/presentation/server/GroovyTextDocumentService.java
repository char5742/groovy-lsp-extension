package com.groovylsp.presentation.server;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.Nullable;

/** Text document service implementation for Groovy files. */
public class GroovyTextDocumentService implements TextDocumentService, LanguageClientAware {

  @Nullable private LanguageClient client;

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    // Will be implemented in future milestones
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    // Will be implemented in future milestones
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    // Will be implemented in future milestones
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    // Will be implemented in future milestones
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Nullable
  protected LanguageClient getClient() {
    return client;
  }
}
