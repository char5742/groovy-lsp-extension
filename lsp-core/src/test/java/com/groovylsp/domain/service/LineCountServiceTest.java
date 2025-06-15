package com.groovylsp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovylsp.testing.FastTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FastTest
class LineCountServiceTest {

  private LineCountService lineCountService;

  @BeforeEach
  void setUp() {
    lineCountService = new LineCountService();
  }

  @Test
  void 空のコンテンツの場合は空の結果を返す() {
    var result = lineCountService.countLines("");

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(0);
    assertThat(lineCount.blankLines()).isEqualTo(0);
    assertThat(lineCount.commentLines()).isEqualTo(0);
    assertThat(lineCount.codeLines()).isEqualTo(0);
  }

  @Test
  void 単一のコード行を正しくカウントできる() {
    var result = lineCountService.countLines("println 'Hello, World!'");

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(1);
    assertThat(lineCount.blankLines()).isEqualTo(0);
    assertThat(lineCount.commentLines()).isEqualTo(0);
    assertThat(lineCount.codeLines()).isEqualTo(1);
  }

  @Test
  void 空行を正しくカウントできる() {
    var content = """
        println 'Hello'

        println 'World'
        """;

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(4);
    assertThat(lineCount.blankLines()).isEqualTo(2);
    assertThat(lineCount.commentLines()).isEqualTo(0);
    assertThat(lineCount.codeLines()).isEqualTo(2);
  }

  @Test
  void 単一行コメントを正しくカウントできる() {
    var content = """
        // これはコメントです
        println 'Hello'
        // 別のコメント
        """;

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(4);
    assertThat(lineCount.blankLines()).isEqualTo(1);
    assertThat(lineCount.commentLines()).isEqualTo(2);
    assertThat(lineCount.codeLines()).isEqualTo(1);
  }

  @Test
  void 複数行コメントを正しくカウントできる() {
    var content =
        """
        /*
         * これは複数行の
         * コメントです
         */
        println 'Hello'
        """;

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(6);
    assertThat(lineCount.blankLines()).isEqualTo(1);
    assertThat(lineCount.commentLines()).isEqualTo(4);
    assertThat(lineCount.codeLines()).isEqualTo(1);
  }

  @Test
  void 混在したコンテンツを正しくカウントできる() {
    var content =
        """
        // Groovyのサンプルコード
        class HelloWorld {

            /* メインメソッド */
            static void main(String[] args) {
                // 挨拶を出力
                println 'Hello, World!'
            }

        }
        """;

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(11);
    assertThat(lineCount.blankLines()).isEqualTo(3);
    assertThat(lineCount.commentLines()).isEqualTo(3);
    assertThat(lineCount.codeLines()).isEqualTo(5);
  }

  @Test
  void Windows形式の改行コードを正しく処理できる() {
    var content = "line1\r\nline2\r\nline3";

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(3);
    assertThat(lineCount.codeLines()).isEqualTo(3);
  }

  @Test
  void 末尾に改行がない場合も正しくカウントできる() {
    var content = "line1\nline2";

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(2);
    assertThat(lineCount.codeLines()).isEqualTo(2);
  }

  @Test
  void スペースのみの行は空行として扱われる() {
    var content = """
        println 'Hello'
           \t
        println 'World'
        """;

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(4);
    assertThat(lineCount.blankLines()).isEqualTo(2);
    assertThat(lineCount.codeLines()).isEqualTo(2);
  }

  @Test
  void nullコンテンツの場合は例外が発生する() {
    assertThatThrownBy(() -> lineCountService.countLines(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("content must not be null");
  }

  // 現在の実装では文字列リテラル内のコメント記号は考慮していない
  // 将来の改善事項として記録

  @Test
  void アスタリスクで始まるコード行はコメントではない() {
    var content = "int result = 5 * 10";

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(1);
    assertThat(lineCount.commentLines()).isEqualTo(0);
    assertThat(lineCount.codeLines()).isEqualTo(1);
  }

  @Test
  void 同一行で開始終了する複数行コメント() {
    var content = "/* inline comment */ println 'Hello'";

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(1);
    // 現在の実装ではコメントが含まれる行全体をコメント行として扱う
    assertThat(lineCount.commentLines()).isEqualTo(1);
    assertThat(lineCount.codeLines()).isEqualTo(0);
  }

  @Test
  void 複数行コメントの終了後にコードがある場合() {
    var content = "/*\n * comment\n */ code";

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(3);
    // 現在の実装ではコメント終了を含む行もコメント行として扱う
    assertThat(lineCount.commentLines()).isEqualTo(3);
    assertThat(lineCount.codeLines()).isEqualTo(0);
  }

  @Test
  void ネストした複数行コメントのテスト() {
    // Groovyではネストした複数行コメントはサポートされていない
    var content = "/*\n * 外側のコメント\n */\ncode /* inline comment */";

    var result = lineCountService.countLines(content);

    assertThat(result.isRight()).isTrue();
    var lineCount = result.get();
    assertThat(lineCount.totalLines()).isEqualTo(4);
    assertThat(lineCount.commentLines()).isEqualTo(3);
    assertThat(lineCount.codeLines()).isEqualTo(1);
  }
}
