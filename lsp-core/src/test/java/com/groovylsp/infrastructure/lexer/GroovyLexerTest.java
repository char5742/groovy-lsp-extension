package com.groovylsp.infrastructure.lexer;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.domain.lexer.Token;
import com.groovylsp.domain.lexer.TokenType;
import com.groovylsp.testing.FastTest;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;

/** GroovyLexerのテスト */
@FastTest
class GroovyLexerTest {

  @Test
  void testキーワードの識別() {
    // Arrange
    String source = "def class if else while for return new";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    // EOFを除いたトークン数を確認
    assertThat(tokens.size()).isEqualTo(9); // 8個のキーワード + EOF

    assertThat(tokens.get(0).type()).isEqualTo(TokenType.DEF);
    assertThat(tokens.get(1).type()).isEqualTo(TokenType.CLASS);
    assertThat(tokens.get(2).type()).isEqualTo(TokenType.IF);
    assertThat(tokens.get(3).type()).isEqualTo(TokenType.ELSE);
    assertThat(tokens.get(4).type()).isEqualTo(TokenType.WHILE);
    assertThat(tokens.get(5).type()).isEqualTo(TokenType.FOR);
    assertThat(tokens.get(6).type()).isEqualTo(TokenType.RETURN);
    assertThat(tokens.get(7).type()).isEqualTo(TokenType.NEW);
    assertThat(tokens.get(8).type()).isEqualTo(TokenType.EOF);
  }

  @Test
  void test文字列リテラルの識別_ダブルクォート() {
    // Arrange
    String source = "\"Hello, World!\"";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens.size()).isEqualTo(2); // 文字列リテラル + EOF
    assertThat(tokens.get(0).type()).isEqualTo(TokenType.STRING_LITERAL);
    assertThat(tokens.get(0).text()).isEqualTo("\"Hello, World!\"");
  }

  @Test
  void test文字列リテラルの識別_シングルクォート() {
    // Arrange
    String source = "'Hello, Groovy!'";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens.size()).isEqualTo(2); // 文字列リテラル + EOF
    assertThat(tokens.get(0).type()).isEqualTo(TokenType.STRING_LITERAL);
    assertThat(tokens.get(0).text()).isEqualTo("'Hello, Groovy!'");
  }

  @Test
  void test数値リテラルの識別() {
    // Arrange
    String source = "42 3.14 100";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens.size()).isEqualTo(4); // 3個の数値 + EOF
    assertThat(tokens.get(0).type()).isEqualTo(TokenType.NUMBER_LITERAL);
    assertThat(tokens.get(0).text()).isEqualTo("42");
    assertThat(tokens.get(1).type()).isEqualTo(TokenType.NUMBER_LITERAL);
    assertThat(tokens.get(1).text()).isEqualTo("3.14");
    assertThat(tokens.get(2).type()).isEqualTo(TokenType.NUMBER_LITERAL);
    assertThat(tokens.get(2).text()).isEqualTo("100");
  }

  @Test
  void test演算子の識別() {
    // Arrange
    String source = "+ - * / % = == != < > <= >= && || !";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens.get(0).type()).isEqualTo(TokenType.PLUS);
    assertThat(tokens.get(1).type()).isEqualTo(TokenType.MINUS);
    assertThat(tokens.get(2).type()).isEqualTo(TokenType.MULTIPLY);
    assertThat(tokens.get(3).type()).isEqualTo(TokenType.DIVIDE);
    assertThat(tokens.get(4).type()).isEqualTo(TokenType.MODULO);
    assertThat(tokens.get(5).type()).isEqualTo(TokenType.ASSIGN);
    assertThat(tokens.get(6).type()).isEqualTo(TokenType.EQUALS);
    assertThat(tokens.get(7).type()).isEqualTo(TokenType.NOT_EQUALS);
    assertThat(tokens.get(8).type()).isEqualTo(TokenType.LESS_THAN);
    assertThat(tokens.get(9).type()).isEqualTo(TokenType.GREATER_THAN);
    assertThat(tokens.get(10).type()).isEqualTo(TokenType.LESS_THAN_OR_EQUAL);
    assertThat(tokens.get(11).type()).isEqualTo(TokenType.GREATER_THAN_OR_EQUAL);
    assertThat(tokens.get(12).type()).isEqualTo(TokenType.AND);
    assertThat(tokens.get(13).type()).isEqualTo(TokenType.OR);
    assertThat(tokens.get(14).type()).isEqualTo(TokenType.NOT);
  }

  @Test
  void test区切り文字の識別() {
    // Arrange
    String source = "(){}[];,.:.";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens.get(0).type()).isEqualTo(TokenType.LEFT_PAREN);
    assertThat(tokens.get(1).type()).isEqualTo(TokenType.RIGHT_PAREN);
    assertThat(tokens.get(2).type()).isEqualTo(TokenType.LEFT_BRACE);
    assertThat(tokens.get(3).type()).isEqualTo(TokenType.RIGHT_BRACE);
    assertThat(tokens.get(4).type()).isEqualTo(TokenType.LEFT_BRACKET);
    assertThat(tokens.get(5).type()).isEqualTo(TokenType.RIGHT_BRACKET);
    assertThat(tokens.get(6).type()).isEqualTo(TokenType.SEMICOLON);
    assertThat(tokens.get(7).type()).isEqualTo(TokenType.COMMA);
    assertThat(tokens.get(8).type()).isEqualTo(TokenType.DOT);
    assertThat(tokens.get(9).type()).isEqualTo(TokenType.COLON);
    assertThat(tokens.get(10).type()).isEqualTo(TokenType.DOT);
  }

  @Test
  void test識別子の識別() {
    // Arrange
    String source = "myVariable _privateVar camelCase CONSTANT";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    assertThat(tokens.size()).isEqualTo(5); // 4個の識別子 + EOF
    assertThat(tokens.get(0).type()).isEqualTo(TokenType.IDENTIFIER);
    assertThat(tokens.get(0).text()).isEqualTo("myVariable");
    assertThat(tokens.get(1).type()).isEqualTo(TokenType.IDENTIFIER);
    assertThat(tokens.get(1).text()).isEqualTo("_privateVar");
    assertThat(tokens.get(2).type()).isEqualTo(TokenType.IDENTIFIER);
    assertThat(tokens.get(2).text()).isEqualTo("camelCase");
    assertThat(tokens.get(3).type()).isEqualTo(TokenType.IDENTIFIER);
    assertThat(tokens.get(3).text()).isEqualTo("CONSTANT");
  }

  @Test
  void testコメントの識別() {
    // Arrange
    String source =
        """
            // 行コメント
            def x = 10
            /* ブロックコメント */
            class Test
            """;
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    // コメントは結果に含まれる
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.COMMENT && t.text().equals("// 行コメント"));
    assertThat(tokens)
        .anyMatch(t -> t.type() == TokenType.COMMENT && t.text().equals("/* ブロックコメント */"));
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.DEF);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.IDENTIFIER && t.text().equals("x"));
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.CLASS);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.IDENTIFIER && t.text().equals("Test"));
  }

  @Test
  void test複雑なGroovyコードの解析() {
    // Arrange
    String source =
        """
            def greet(String name) {
                if (name != null) {
                    return "Hello, " + name + "!"
                } else {
                    return "Hello, World!"
                }
            }
            """;
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    // 主要なトークンの存在を確認
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.DEF);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.IDENTIFIER && t.text().equals("greet"));
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.IDENTIFIER && t.text().equals("String"));
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.IDENTIFIER && t.text().equals("name"));
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.IF);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.NOT_EQUALS);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.NULL);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.RETURN);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.STRING_LITERAL);
    assertThat(tokens).anyMatch(t -> t.type() == TokenType.ELSE);
  }

  @Test
  void testトークンの位置情報() {
    // Arrange
    String source = "def x = 42";
    var lexer = new GroovyLexer(source);

    // Act
    Either<String, List<Token>> result = lexer.tokenize();

    // Assert
    assertThat(result.isRight()).isTrue();
    List<Token> tokens = result.get();

    Token defToken = tokens.get(0);
    assertThat(defToken.startPosition()).isEqualTo(0);
    assertThat(defToken.endPosition()).isEqualTo(3);
    assertThat(defToken.line()).isEqualTo(1);
    assertThat(defToken.column()).isEqualTo(1);

    Token xToken = tokens.get(1);
    assertThat(xToken.startPosition()).isEqualTo(4);
    assertThat(xToken.endPosition()).isEqualTo(5);
    assertThat(xToken.line()).isEqualTo(1);
    assertThat(xToken.column()).isEqualTo(5);
  }
}
