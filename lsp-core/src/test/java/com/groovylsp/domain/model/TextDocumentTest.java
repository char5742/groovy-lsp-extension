package com.groovylsp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.groovylsp.testing.FastTest;
import java.net.URI;
import org.junit.jupiter.api.Test;

@FastTest
class TextDocumentTest {

  @Test
  void shouldCreateTextDocument() {
    var uri = URI.create("file:///test.groovy");
    var document = new TextDocument(uri, "groovy", 1, "class Test {}");

    assertNotNull(document);
    assertEquals(uri, document.uri());
    assertEquals("groovy", document.languageId());
    assertEquals(1, document.version());
    assertEquals("class Test {}", document.content());
  }

  @Test
  void shouldThrowExceptionForNullUri() {
    assertThrows(NullPointerException.class, () -> new TextDocument(null, "groovy", 1, "content"));
  }

  @Test
  void shouldThrowExceptionForNullLanguageId() {
    var uri = URI.create("file:///test.groovy");
    assertThrows(NullPointerException.class, () -> new TextDocument(uri, null, 1, "content"));
  }

  @Test
  void shouldThrowExceptionForNullContent() {
    var uri = URI.create("file:///test.groovy");
    assertThrows(NullPointerException.class, () -> new TextDocument(uri, "groovy", 1, null));
  }

  @Test
  void shouldThrowExceptionForNegativeVersion() {
    var uri = URI.create("file:///test.groovy");
    assertThrows(
        IllegalArgumentException.class, () -> new TextDocument(uri, "groovy", -1, "content"));
  }

  @Test
  void shouldCreateNewDocumentWithUpdatedContent() {
    var uri = URI.create("file:///test.groovy");
    var original = new TextDocument(uri, "groovy", 1, "class Test {}");

    var updated = original.withContent("class Test { void test() {} }", 2);

    assertEquals(uri, updated.uri());
    assertEquals("groovy", updated.languageId());
    assertEquals(2, updated.version());
    assertEquals("class Test { void test() {} }", updated.content());
    assertEquals("class Test {}", original.content());
  }

  @Test
  void shouldCreateNewDocumentWithUpdatedVersion() {
    var uri = URI.create("file:///test.groovy");
    var original = new TextDocument(uri, "groovy", 1, "class Test {}");

    var updated = original.withVersion(2);

    assertEquals(uri, updated.uri());
    assertEquals("groovy", updated.languageId());
    assertEquals(2, updated.version());
    assertEquals("class Test {}", updated.content());
    assertEquals(1, original.version());
  }
}
