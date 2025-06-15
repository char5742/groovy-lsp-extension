package com.groovylsp.domain.repository;

import com.groovylsp.domain.model.TextDocument;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.net.URI;
import java.util.Collection;

public interface TextDocumentRepository {

  Either<DocumentError, TextDocument> save(TextDocument document);

  Option<TextDocument> findByUri(URI uri);

  Either<DocumentError, TextDocument> remove(URI uri);

  Collection<TextDocument> findAll();

  void clear();

  sealed interface DocumentError {
    record DocumentNotFound(URI uri) implements DocumentError {}

    record InvalidDocument(String message) implements DocumentError {}
  }
}
