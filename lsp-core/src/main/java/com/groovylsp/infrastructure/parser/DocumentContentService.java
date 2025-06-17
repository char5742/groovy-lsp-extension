package com.groovylsp.infrastructure.parser;

import com.groovylsp.domain.repository.TextDocumentRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.net.URI;
import javax.inject.Inject;

/** ドキュメントのコンテンツを取得するサービス */
public class DocumentContentService {

  private final TextDocumentRepository textDocumentRepository;

  @Inject
  public DocumentContentService(TextDocumentRepository textDocumentRepository) {
    this.textDocumentRepository = textDocumentRepository;
  }

  /**
   * 指定URIのドキュメントコンテンツを取得
   *
   * @param uri ファイルURI
   * @return ドキュメントの内容
   */
  public Option<String> getContent(String uri) {
    return Try.of(() -> URI.create(uri))
        .toOption()
        .flatMap(textDocumentRepository::findByUri)
        .map(doc -> doc.content());
  }
}
