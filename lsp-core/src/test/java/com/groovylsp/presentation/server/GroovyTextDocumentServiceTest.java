package com.groovylsp.presentation.server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.groovylsp.application.usecase.DiagnosticUseCase;
import com.groovylsp.application.usecase.TextDocumentSyncUseCase;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import java.net.URI;
import java.util.List;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FastTest
class GroovyTextDocumentServiceTest {

  private GroovyTextDocumentService service;
  private TextDocumentSyncUseCase syncUseCase;
  private DiagnosticUseCase diagnosticUseCase;
  private LanguageClient client;

  @BeforeEach
  void setUp() {
    syncUseCase = mock(TextDocumentSyncUseCase.class);
    diagnosticUseCase = mock(DiagnosticUseCase.class);
    client = mock(LanguageClient.class);
    service = new GroovyTextDocumentService(syncUseCase, diagnosticUseCase);
    service.connect(client);
  }

  @Test
  void shouldCallOpenDocumentUseCase() {
    var uri = "file:///test.groovy";
    var textDocument = new TextDocumentItem(uri, "groovy", 1, "class Test {}");
    var params = new DidOpenTextDocumentParams(textDocument);
    var document = new TextDocument(URI.create(uri), "groovy", 1, "class Test {}");

    when(syncUseCase.openDocument(params)).thenReturn(Either.right(document));
    when(diagnosticUseCase.diagnose(document))
        .thenReturn(
            Either.right(com.groovylsp.domain.model.DiagnosticResult.empty(URI.create(uri))));

    service.didOpen(params);

    verify(syncUseCase).openDocument(params);
  }

  @Test
  void shouldHandleOpenDocumentError() {
    var uri = "file:///test.groovy";
    var textDocument = new TextDocumentItem(uri, "groovy", 1, "class Test {}");
    var params = new DidOpenTextDocumentParams(textDocument);
    var error = new TextDocumentSyncUseCase.SyncError.UnexpectedError("Error");

    when(syncUseCase.openDocument(params)).thenReturn(Either.left(error));

    service.didOpen(params);

    verify(syncUseCase).openDocument(params);
  }

  @Test
  void shouldCallChangeDocumentUseCase() {
    var uri = "file:///test.groovy";
    var changeEvent = new TextDocumentContentChangeEvent();
    changeEvent.setText("class Test { void test() {} }");
    var identifier = new VersionedTextDocumentIdentifier(uri, 2);
    var params = new DidChangeTextDocumentParams(identifier, List.of(changeEvent));
    var document = new TextDocument(URI.create(uri), "groovy", 2, "class Test { void test() {} }");

    when(syncUseCase.changeDocument(params)).thenReturn(Either.right(document));
    when(diagnosticUseCase.diagnose(document))
        .thenReturn(
            Either.right(com.groovylsp.domain.model.DiagnosticResult.empty(URI.create(uri))));

    service.didChange(params);

    verify(syncUseCase).changeDocument(params);
  }

  @Test
  void shouldHandleChangeDocumentError() {
    var uri = "file:///test.groovy";
    var changeEvent = new TextDocumentContentChangeEvent();
    changeEvent.setText("new content");
    var identifier = new VersionedTextDocumentIdentifier(uri, 2);
    var params = new DidChangeTextDocumentParams(identifier, List.of(changeEvent));
    var error = new TextDocumentSyncUseCase.SyncError.DocumentNotFound(URI.create(uri));

    when(syncUseCase.changeDocument(params)).thenReturn(Either.left(error));

    service.didChange(params);

    verify(syncUseCase).changeDocument(params);
  }

  @Test
  void shouldCallCloseDocumentUseCase() {
    var uri = "file:///test.groovy";
    var identifier = new TextDocumentIdentifier(uri);
    var params = new DidCloseTextDocumentParams(identifier);

    when(syncUseCase.closeDocument(params)).thenReturn(Either.right(URI.create(uri)));

    service.didClose(params);

    verify(syncUseCase).closeDocument(params);
  }

  @Test
  void shouldHandleCloseDocumentError() {
    var uri = "file:///test.groovy";
    var identifier = new TextDocumentIdentifier(uri);
    var params = new DidCloseTextDocumentParams(identifier);
    var error = new TextDocumentSyncUseCase.SyncError.RepositoryError("Error");

    when(syncUseCase.closeDocument(params)).thenReturn(Either.left(error));

    service.didClose(params);

    verify(syncUseCase).closeDocument(params);
  }

  @Test
  void shouldHandleSaveDocument() {
    var uri = "file:///test.groovy";
    var identifier = new TextDocumentIdentifier(uri);
    var params = new DidSaveTextDocumentParams(identifier);

    service.didSave(params);
  }
}
