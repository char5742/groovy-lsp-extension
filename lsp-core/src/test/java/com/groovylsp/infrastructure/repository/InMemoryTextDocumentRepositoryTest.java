package com.groovylsp.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.testing.FastTest;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FastTest
class InMemoryTextDocumentRepositoryTest {

  private InMemoryTextDocumentRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryTextDocumentRepository();
  }

  @Test
  void shouldSaveDocument() {
    var uri = URI.create("file:///test.groovy");
    var document = new TextDocument(uri, "groovy", 1, "class Test {}");

    var result = repository.save(document);

    assertTrue(result.isRight());
    assertEquals(document, result.get());
  }

  @Test
  void shouldFindDocumentByUri() {
    var uri = URI.create("file:///test.groovy");
    var document = new TextDocument(uri, "groovy", 1, "class Test {}");
    repository.save(document);

    var result = repository.findByUri(uri);

    assertTrue(result.isDefined());
    assertEquals(document, result.get());
  }

  @Test
  void shouldReturnEmptyWhenDocumentNotFound() {
    var uri = URI.create("file:///notfound.groovy");

    var result = repository.findByUri(uri);

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldRemoveDocument() {
    var uri = URI.create("file:///test.groovy");
    var document = new TextDocument(uri, "groovy", 1, "class Test {}");
    repository.save(document);

    var result = repository.remove(uri);

    assertTrue(result.isRight());
    assertEquals(document, result.get());
    assertTrue(repository.findByUri(uri).isEmpty());
  }

  @Test
  void shouldReturnErrorWhenRemovingNonExistentDocument() {
    var uri = URI.create("file:///notfound.groovy");

    var result = repository.remove(uri);

    assertTrue(result.isLeft());
    assertTrue(result.getLeft() instanceof TextDocumentRepository.DocumentError.DocumentNotFound);
  }

  @Test
  void shouldFindAllDocuments() {
    var uri1 = URI.create("file:///test1.groovy");
    var uri2 = URI.create("file:///test2.groovy");
    var doc1 = new TextDocument(uri1, "groovy", 1, "class Test1 {}");
    var doc2 = new TextDocument(uri2, "groovy", 1, "class Test2 {}");
    repository.save(doc1);
    repository.save(doc2);

    var result = repository.findAll();

    assertEquals(2, result.size());
    assertTrue(result.contains(doc1));
    assertTrue(result.contains(doc2));
  }

  @Test
  void shouldClearAllDocuments() {
    var uri1 = URI.create("file:///test1.groovy");
    var uri2 = URI.create("file:///test2.groovy");
    var doc1 = new TextDocument(uri1, "groovy", 1, "class Test1 {}");
    var doc2 = new TextDocument(uri2, "groovy", 1, "class Test2 {}");
    repository.save(doc1);
    repository.save(doc2);

    repository.clear();

    assertTrue(repository.findAll().isEmpty());
    assertTrue(repository.findByUri(uri1).isEmpty());
    assertTrue(repository.findByUri(uri2).isEmpty());
  }

  @Test
  void shouldUpdateExistingDocument() {
    var uri = URI.create("file:///test.groovy");
    var original = new TextDocument(uri, "groovy", 1, "class Test {}");
    var updated = new TextDocument(uri, "groovy", 2, "class Test { void test() {} }");

    repository.save(original);
    repository.save(updated);

    var result = repository.findByUri(uri);
    assertTrue(result.isDefined());
    assertEquals(updated, result.get());
    assertEquals(2, result.get().version());
  }
}
