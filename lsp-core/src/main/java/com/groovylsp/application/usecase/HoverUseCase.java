package com.groovylsp.application.usecase;

import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.TypeInfoService;
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
  private final TypeInfoService typeInfoService;

  @Inject
  public HoverUseCase(TextDocumentRepository repository, TypeInfoService typeInfoService) {
    this.repository = repository;
    this.typeInfoService = typeInfoService;
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
        .flatMap(
            document -> {
              // 型情報を取得
              Either<String, TypeInfoService.TypeInfo> typeInfoResult =
                  typeInfoService.getTypeInfoAt(uri, document.content(), params.getPosition());

              if (typeInfoResult.isRight()) {
                // 型情報が見つかった場合
                TypeInfoService.TypeInfo typeInfo = typeInfoResult.get();
                var content = new MarkupContent();
                content.setKind(MarkupKind.MARKDOWN);
                content.setValue(formatTypeInfo(typeInfo));

                var hover = new Hover();
                hover.setContents(content);
                return Either.right(hover);
              } else {
                // エラーメッセージから適切なフィードバックを作成
                String message = typeInfoResult.getLeft();

                // エラーに応じた適切なメッセージを設定
                String hoverText;
                if (message.contains("識別子が見つかりません") || message.contains("識別子が特定できません")) {
                  // 識別子がない場合はホバーを表示しない
                  return Either.right(null);
                } else if (message.contains("パースエラー")) {
                  hoverText = "構文エラーのため型情報を取得できません";
                } else if (message.contains("定義が見つかりません")) {
                  // 定義が見つからない場合も、より具体的なメッセージ
                  hoverText = "定義が見つかりません";
                } else if (message.contains("型情報が見つかりません")) {
                  // 型情報が見つからない場合は Groovy element を表示
                  hoverText = "Groovy element";
                } else {
                  // その他のエラーの場合
                  hoverText = "型情報を取得できません";
                }

                var content = new MarkupContent();
                content.setKind(MarkupKind.PLAINTEXT);
                content.setValue(hoverText);

                var hover = new Hover();
                hover.setContents(content);
                return Either.right(hover);
              }
            });
  }

  /**
   * 型情報をMarkdown形式でフォーマット
   *
   * @param typeInfo 型情報
   * @return フォーマットされた文字列
   */
  private String formatTypeInfo(TypeInfoService.TypeInfo typeInfo) {
    var sb = new StringBuilder();

    // 型情報のヘッダー
    if (typeInfo.modifiers() != null) {
      sb.append("```groovy\n");
      sb.append(typeInfo.modifiers()).append(" ");
    } else {
      sb.append("```groovy\n");
    }

    // 種類に応じたフォーマット
    switch (typeInfo.kind()) {
      case LOCAL_VARIABLE:
      case FIELD:
      case PARAMETER:
        sb.append(typeInfo.type()).append(" ").append(typeInfo.name());
        break;
      case METHOD:
        sb.append(typeInfo.type()); // メソッドの場合はシグネチャ全体が入っている
        break;
      case CLASS:
        sb.append("class ").append(typeInfo.name());
        break;
    }

    sb.append("\n```");

    // ドキュメントがあれば追加
    if (typeInfo.documentation() != null) {
      sb.append("\n\n").append(typeInfo.documentation());
    }

    return sb.toString();
  }
}
