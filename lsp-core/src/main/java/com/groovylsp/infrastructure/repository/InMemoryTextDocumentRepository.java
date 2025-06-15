package com.groovylsp.infrastructure.repository;

import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InMemoryTextDocumentRepository implements TextDocumentRepository {

  private final ConcurrentMap<URI, TextDocument> documents = new ConcurrentHashMap<>();

  @Inject
  public InMemoryTextDocumentRepository() {}

  @Override
  public Either<DocumentError, TextDocument> save(TextDocument document) {
    var previous = documents.get(document.uri());

    if (previous != null && previous.version() >= document.version()) {
      // 古いバージョンは無視してエラーを返す
      return Either.left(
          new DocumentError.InvalidDocument(
              "バージョン競合: 既存バージョン "
                  + previous.version()
                  + " は提供されたバージョン "
                  + document.version()
                  + " より新しいか同じです"));
    }

    documents.put(document.uri(), document);
    return Either.right(document);
  }

  @Override
  public Option<TextDocument> findByUri(URI uri) {
    return Option.of(documents.get(uri));
  }

  @Override
  public Either<DocumentError, TextDocument> remove(URI uri) {
    var removed = documents.remove(uri);
    if (removed == null) {
      return Either.left(new DocumentError.DocumentNotFound(uri));
    }
    return Either.right(removed);
  }

  @Override
  public Collection<TextDocument> findAll() {
    return documents.values();
  }

  /**
   * リポジトリからすべてのドキュメントをクリア。警告: このメソッドはテスト目的でのみ使用してください。 本番環境では、remove()メソッドを使用してドキュメントを個別に削除してください。
   */
  @Override
  public void clear() {
    documents.clear();
  }
}
