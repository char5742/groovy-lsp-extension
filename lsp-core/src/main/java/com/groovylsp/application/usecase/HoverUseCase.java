package com.groovylsp.application.usecase;

import com.groovylsp.domain.repository.TextDocumentRepository;
import io.vavr.control.Either;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ホバー情報の取得に関するユースケース
 *
 * <p>LSPのtextDocument/hoverリクエストを処理し、 カーソル位置の要素情報を提供します。
 */
@Singleton
public class HoverUseCase {

  private static final Logger logger = LoggerFactory.getLogger(HoverUseCase.class);

  private final TextDocumentRepository repository;

  @Inject
  public HoverUseCase(TextDocumentRepository repository) {
    this.repository = repository;
  }

  /**
   * ホバー情報を取得
   *
   * @param params HoverParams
   * @return ホバー情報、またはエラー
   */
  public Either<String, Hover> getHover(HoverParams params) {
    String uri = params.getTextDocument().getUri();
    logger.debug(
        "ホバー情報を取得: {} at {}:{}",
        uri,
        params.getPosition().getLine(),
        params.getPosition().getCharacter());

    return repository
        .findByUri(URI.create(uri))
        .toEither(() -> "ドキュメントが見つかりません: " + uri)
        .map(
            document -> {
              // 現時点では固定テキストを返す
              var content = new MarkupContent();
              content.setKind(MarkupKind.PLAINTEXT);
              content.setValue("Groovy element");

              var hover = new Hover();
              hover.setContents(content);
              return hover;
            });
  }
}
