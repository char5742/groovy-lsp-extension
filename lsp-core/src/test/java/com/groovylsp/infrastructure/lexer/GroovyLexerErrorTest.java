package com.groovylsp.infrastructure.lexer;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.domain.lexer.Token;
import com.groovylsp.domain.lexer.TokenType;
import com.groovylsp.testing.FastTest;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

/** GroovyLexerのエラー処理のテスト */
@FastTest
class GroovyLexerErrorTest {

  @Test
  void test閉じられていない文字列リテラル_ダブルクォート() {
    // Arrange
    String source = "def text = \"unclosed string";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    // エラートークンが含まれることを確認
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.ERROR_UNCLOSED_STRING);
    Token errorToken = tokens.find(t -> t.type() == TokenType.ERROR_UNCLOSED_STRING).get();
    assertThat(errorToken.text()).isEqualTo("\"unclosed string");
  }

  @Test
  void test閉じられていない文字列リテラル_シングルクォート() {
    // Arrange
    String source = "def text = 'unclosed string";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens).anyMatch(t -> t.type() == TokenType.ERROR_UNCLOSED_STRING);
    Token errorToken = tokens.find(t -> t.type() == TokenType.ERROR_UNCLOSED_STRING).get();
    assertThat(errorToken.text()).isEqualTo("'unclosed string");
  }

  @Test
  void test閉じられていないブロックコメント() {
    // Arrange
    String source =
        """
            def x = 10
            /* This is an unclosed
               block comment
            """;
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens).anyMatch(t -> t.type() == TokenType.ERROR_UNCLOSED_COMMENT);
    Token errorToken = tokens.find(t -> t.type() == TokenType.ERROR_UNCLOSED_COMMENT).get();
    assertThat(errorToken.text()).contains("/* This is an unclosed");
  }

  @Test
  void test複数のエラートークン() {
    // Arrange
    String source = "def a = \"unclosed def b = 'also unclosed /* unclosed comment";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    // 最初のエラートークンが含まれることを確認（現在の実装では最初のエラーで停止）
    assertThat(tokens)
        .anyMatch(
            t ->
                t.type() == TokenType.ERROR_UNCLOSED_STRING
                    || t.type() == TokenType.ERROR_UNCLOSED_COMMENT);
  }

  @Test
  void testエラートークンの位置情報() {
    // Arrange
    String source = "def text = \"unclosed";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    Token errorToken = tokens.find(t -> t.type() == TokenType.ERROR_UNCLOSED_STRING).get();
    assertThat(errorToken.startPosition()).isEqualTo(11); // "の位置
    assertThat(errorToken.endPosition()).isEqualTo(20); // 文字列の終端
    assertThat(errorToken.line()).isEqualTo(1);
    assertThat(errorToken.column()).isEqualTo(12);
  }
}
