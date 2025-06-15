package com.groovylsp.domain.model;

import java.net.URI;
import java.util.Objects;

public record TextDocument(URI uri, String languageId, int version, String content) {

  public TextDocument {
    Objects.requireNonNull(uri, "uri must not be null");
    Objects.requireNonNull(languageId, "languageId must not be null");
    Objects.requireNonNull(content, "content must not be null");
    if (version < 0) {
      throw new IllegalArgumentException("version must be non-negative");
    }
  }

  public TextDocument withContent(String newContent, int newVersion) {
    return new TextDocument(uri, languageId, newVersion, newContent);
  }

  public TextDocument withVersion(int newVersion) {
    return new TextDocument(uri, languageId, newVersion, content);
  }
}
