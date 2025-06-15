package com.groovylsp.presentation.server;

import com.groovylsp.application.usecase.TextDocumentSyncUseCase;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Text document service implementation for Groovy files. */
@Singleton
public class GroovyTextDocumentService implements TextDocumentService, LanguageClientAware {

  private static final Logger logger = LoggerFactory.getLogger(GroovyTextDocumentService.class);

  private @Nullable LanguageClient client;
  private final TextDocumentSyncUseCase syncUseCase;

  @Inject
  public GroovyTextDocumentService(TextDocumentSyncUseCase syncUseCase) {
    this.syncUseCase = syncUseCase;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    syncUseCase
        .openDocument(params)
        .peek(document -> logger.debug("Successfully opened document: {}", document.uri()))
        .peekLeft(error -> logger.error("Failed to open document: {}", error));
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    syncUseCase
        .changeDocument(params)
        .peek(
            document ->
                logger.debug(
                    "Successfully changed document: {} (version: {})",
                    document.uri(),
                    document.version()))
        .peekLeft(error -> logger.error("Failed to change document: {}", error));
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    syncUseCase
        .closeDocument(params)
        .peek(uri -> logger.debug("Successfully closed document: {}", uri))
        .peekLeft(error -> logger.error("Failed to close document: {}", error));
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    logger.debug("Document saved: {}", params.getTextDocument().getUri());
  }

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  protected @Nullable LanguageClient getClient() {
    return client;
  }
}
