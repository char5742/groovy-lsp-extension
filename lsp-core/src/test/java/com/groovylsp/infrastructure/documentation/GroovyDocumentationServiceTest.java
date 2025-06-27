package com.groovylsp.infrastructure.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.domain.model.Documentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * GroovyDocumentationServiceのテスト
 */
class GroovyDocumentationServiceTest {

  private GroovyDocumentationService documentationService;

  @BeforeEach
  void setUp() {
    documentationService = new GroovyDocumentationService();
  }

  @Test
  void parseDocumentationComment_基本的なコメント() {
    String comment = """
        /**
         * これはサンプルクラスです。
         * 詳細な説明がここに続きます。
         *
         * @param name 名前パラメータ
         * @param age 年齢パラメータ
         * @return 処理結果
         * @throws IllegalArgumentException 引数が不正な場合
         * @since 1.0
         */
        """;

    Documentation doc = documentationService.parseDocumentationComment(comment);

    assertThat(doc.summary()).isEqualTo("これはサンプルクラスです。");
    assertThat(doc.description().isDefined()).isTrue();
    assertThat(doc.description().get()).isEqualTo("詳細な説明がここに続きます。");
    assertThat(doc.params()).hasSize(2);
    assertThat(doc.params().get(0).name()).isEqualTo("name");
    assertThat(doc.params().get(0).description()).isEqualTo("名前パラメータ");
    assertThat(doc.returns().isDefined()).isTrue();
    assertThat(doc.returns().get()).isEqualTo("処理結果");
    assertThat(doc.exceptions()).hasSize(1);
    assertThat(doc.exceptions().get(0).exceptionType()).isEqualTo("IllegalArgumentException");
    assertThat(doc.tags()).containsKey("since");
    assertThat(doc.tags().get("since")).isEqualTo("1.0");
  }

  @Test
  void parseDocumentationComment_サマリーのみ() {
    String comment = """
        /**
         * 単純なサマリーです。
         */
        """;

    Documentation doc = documentationService.parseDocumentationComment(comment);

    assertThat(doc.summary()).isEqualTo("単純なサマリーです。");
    assertThat(doc.description().isEmpty()).isTrue();
    assertThat(doc.params()).isEmpty();
    assertThat(doc.returns().isEmpty()).isTrue();
    assertThat(doc.exceptions()).isEmpty();
    assertThat(doc.tags()).isEmpty();
  }

  @Test
  void parseDocumentationComment_空のコメント() {
    Documentation doc = documentationService.parseDocumentationComment("");

    assertThat(doc.isEmpty()).isTrue();
  }

  @Test
  void formatDocumentation_完全なドキュメント() {
    var doc = new Documentation(
        "サンプルメソッド",
        io.vavr.control.Option.of("このメソッドは何かを行います。"),
        java.util.List.of(
            new Documentation.ParamDoc("input", "入力値"),
            new Documentation.ParamDoc("options", "オプション設定")),
        io.vavr.control.Option.of("処理結果"),
        java.util.List.of(new Documentation.ThrowsDoc("RuntimeException", "処理に失敗した場合")),
        java.util.Map.of("since", "1.0", "author", "開発者"));

    String formatted = documentationService.formatDocumentation(doc);

    assertThat(formatted).contains("サンプルメソッド");
    assertThat(formatted).contains("このメソッドは何かを行います。");
    assertThat(formatted).contains("**Parameters:**");
    assertThat(formatted).contains("- `input`: 入力値");
    assertThat(formatted).contains("**Returns:** 処理結果");
    assertThat(formatted).contains("**Throws:**");
    assertThat(formatted).contains("- `RuntimeException`: 処理に失敗した場合");
    assertThat(formatted).contains("**Additional Info:**");
  }

  @Test
  void formatDocumentation_サマリーのみ() {
    Documentation doc = Documentation.withSummary("シンプルなメソッド");

    String formatted = documentationService.formatDocumentation(doc);

    assertThat(formatted).isEqualTo("シンプルなメソッド");
  }
}