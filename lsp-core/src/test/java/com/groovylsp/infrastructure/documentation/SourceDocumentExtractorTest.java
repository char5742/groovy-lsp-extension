package com.groovylsp.infrastructure.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.codehaus.groovy.ast.ASTNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SourceDocumentExtractorのテスト
 */
class SourceDocumentExtractorTest {

  private SourceDocumentExtractor extractor;

  @BeforeEach
  void setUp() {
    extractor = new SourceDocumentExtractor(new GroovyDocumentationService());
  }

  @Test
  void extractDocCommentFromLines_単一行ドキュメントコメント() {
    var lines = List.of(
        "package com.example",
        "",
        "/** 単純なクラスです */",
        "class MyClass {",
        "}"
    );

    var fakeNode = new FakeASTNode(4); // class MyClass の行番号

    var result = extractor.extractDocCommentFromLines(fakeNode, lines);

    assertThat(result.isDefined()).isTrue();
    assertThat(result.get()).isEqualTo("/** 単純なクラスです */");
  }

  @Test
  void extractDocCommentFromLines_複数行ドキュメントコメント() {
    var lines = List.of(
        "package com.example",
        "",
        "/**",
        " * 複数行のドキュメントコメントです。",
        " * 詳細な説明がここに続きます。",
        " *",
        " * @param name 名前パラメータ",
        " * @return 処理結果",
        " */",
        "class MyClass {",
        "}"
    );

    var fakeNode = new FakeASTNode(10); // class MyClass の行番号

    var result = extractor.extractDocCommentFromLines(fakeNode, lines);

    assertThat(result.isDefined()).isTrue();
    assertThat(result.get()).contains("複数行のドキュメントコメントです。");
    assertThat(result.get()).contains("@param name 名前パラメータ");
  }

  @Test
  void extractDocCommentFromLines_ドキュメントコメントなし() {
    var lines = List.of(
        "package com.example",
        "",
        "// 通常のコメント",
        "class MyClass {",
        "}"
    );

    var fakeNode = new FakeASTNode(4); // class MyClass の行番号

    var result = extractor.extractDocCommentFromLines(fakeNode, lines);

    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void extractDocCommentFromLines_メソッドのドキュメント() {
    var lines = List.of(
        "class MyClass {",
        "",
        "    /**",
        "     * メソッドの説明です。",
        "     * @param input 入力値",
        "     * @return 処理結果",
        "     */",
        "    def processData(input) {",
        "        return input * 2",
        "    }",
        "}"
    );

    var fakeNode = new FakeASTNode(8); // def processData の行番号

    var result = extractor.extractDocCommentFromLines(fakeNode, lines);

    assertThat(result.isDefined()).isTrue();
    assertThat(result.get()).contains("メソッドの説明です。");
    assertThat(result.get()).contains("@param input 入力値");
  }

  @Test
  void extractDocCommentFromSource_統合テスト() {
    String sourceCode = """
        package com.example
        
        /**
         * サンプルクラスです。
         * このクラスは何かを行います。
         *
         * @since 1.0
         * @author 開発者
         */
        class SampleClass {
            
            /**
             * フィールドの説明です。
             */
            String name
            
            /**
             * メソッドの説明です。
             * @param value 入力値
             * @return 処理結果
             */
            def process(value) {
                return value
            }
        }
        """;

    // クラスのドキュメント
    var classDoc = extractor.extractDocCommentFromSource(sourceCode, 10);
    assertThat(classDoc.isDefined()).isTrue();
    assertThat(classDoc.get()).contains("サンプルクラスです。");

    // フィールドのドキュメント
    var fieldDoc = extractor.extractDocCommentFromSource(sourceCode, 15);
    assertThat(fieldDoc.isDefined()).isTrue();
    assertThat(fieldDoc.get()).contains("フィールドの説明です。");

    // メソッドのドキュメント  
    var methodDoc = extractor.extractDocCommentFromSource(sourceCode, 22);
    assertThat(methodDoc.isDefined()).isTrue();
    assertThat(methodDoc.get()).contains("メソッドの説明です。");
  }

  // テスト用の簡単なASTNode実装
  private static class FakeASTNode extends ASTNode {
    private final int lineNumber;

    public FakeASTNode(int lineNumber) {
      this.lineNumber = lineNumber;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }
  }
}