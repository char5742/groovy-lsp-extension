package com.groovylsp.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.testing.FastTest;
import java.net.URI;
import java.util.List;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FastTest
class TextDocumentSyncUseCaseTest {

  private TextDocumentSyncUseCase useCase;
  private InMemoryTextDocumentRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryTextDocumentRepository();
    useCase = new TextDocumentSyncUseCase(repository);
  }

  @Test
  void shouldOpenDocument() {
    var uri = "file:///test.groovy";
    var textDocument = new TextDocumentItem(uri, "groovy", 1, "class Test {}");
    var params = new DidOpenTextDocumentParams(textDocument);

    var result = useCase.openDocument(params);

    assertTrue(result.isRight());
    var document = result.get();
    assertEquals(URI.create(uri), document.uri());
    assertEquals("groovy", document.languageId());
    assertEquals(1, document.version());
    assertEquals("class Test {}", document.content());

    var saved = repository.findByUri(URI.create(uri));
    assertTrue(saved.isDefined());
    assertEquals(document, saved.get());
  }

  @Test
  void shouldChangeDocument() {
    var uri = "file:///test.groovy";
    var textDocument = new TextDocumentItem(uri, "groovy", 1, "class Test {}");
    var openParams = new DidOpenTextDocumentParams(textDocument);
    useCase.openDocument(openParams);

    var changeEvent = new TextDocumentContentChangeEvent();
    changeEvent.setText("class Test { void test() {} }");
    var identifier = new VersionedTextDocumentIdentifier(uri, 2);
    var changeParams = new DidChangeTextDocumentParams(identifier, List.of(changeEvent));

    var result = useCase.changeDocument(changeParams);

    assertTrue(result.isRight());
    var document = result.get();
    assertEquals(URI.create(uri), document.uri());
    assertEquals(2, document.version());
    assertEquals("class Test { void test() {} }", document.content());
  }

  @Test
  void shouldReturnErrorWhenChangingNonExistentDocument() {
    var uri = "file:///notfound.groovy";
    var changeEvent = new TextDocumentContentChangeEvent();
    changeEvent.setText("new content");
    var identifier = new VersionedTextDocumentIdentifier(uri, 1);
    var params = new DidChangeTextDocumentParams(identifier, List.of(changeEvent));

    var result = useCase.changeDocument(params);

    assertTrue(result.isLeft());
    assertTrue(result.getLeft() instanceof TextDocumentSyncUseCase.SyncError.DocumentNotFound);
  }

  @Test
  void shouldCloseDocument() {
    var uri = "file:///test.groovy";
    var textDocument = new TextDocumentItem(uri, "groovy", 1, "class Test {}");
    var openParams = new DidOpenTextDocumentParams(textDocument);
    useCase.openDocument(openParams);

    var identifier = new TextDocumentIdentifier(uri);
    var closeParams = new DidCloseTextDocumentParams(identifier);

    var result = useCase.closeDocument(closeParams);

    assertTrue(result.isRight());
    assertEquals(URI.create(uri), result.get());

    var document = repository.findByUri(URI.create(uri));
    assertTrue(document.isEmpty());
  }

  @Test
  void shouldReturnErrorWhenClosingNonExistentDocument() {
    var uri = "file:///notfound.groovy";
    var identifier = new TextDocumentIdentifier(uri);
    var params = new DidCloseTextDocumentParams(identifier);

    var result = useCase.closeDocument(params);

    assertTrue(result.isLeft());
    assertTrue(result.getLeft() instanceof TextDocumentSyncUseCase.SyncError.RepositoryError);
  }

  @Test
  void shouldHandleMultipleChanges() {
    var uri = "file:///test.groovy";
    var textDocument = new TextDocumentItem(uri, "groovy", 1, "class Test {}");
    var openParams = new DidOpenTextDocumentParams(textDocument);
    useCase.openDocument(openParams);

    var change1 = new TextDocumentContentChangeEvent();
    change1.setText("class Test { void test1() {} }");
    var change2 = new TextDocumentContentChangeEvent();
    change2.setText("class Test { void test2() {} }");
    var identifier = new VersionedTextDocumentIdentifier(uri, 2);
    var changeParams = new DidChangeTextDocumentParams(identifier, List.of(change1, change2));

    var result = useCase.changeDocument(changeParams);

    assertTrue(result.isRight());
    var document = result.get();
    assertEquals("class Test { void test2() {} }", document.content());
  }
}
