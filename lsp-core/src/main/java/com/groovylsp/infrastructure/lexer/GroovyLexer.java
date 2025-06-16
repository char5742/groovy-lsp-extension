package com.groovylsp.infrastructure.lexer;

import com.groovylsp.domain.lexer.Token;
import com.groovylsp.domain.lexer.TokenType;
import io.vavr.collection.List;
import io.vavr.control.Either;
import java.util.HashMap;
import java.util.Map;

/** Groovyの字句解析器 */
public class GroovyLexer {

  // Groovyのキーワード数に基づいた初期容量
  private static final Map<String, TokenType> KEYWORDS = new HashMap<>(64);

  static {
    // キーワードの登録
    for (TokenType type : TokenType.values()) {
      if (type.isKeyword()) {
        KEYWORDS.put(type.getKeyword(), type);
      }
    }
  }

  private final String source;
  private int position;
  private int line;
  private int column;

  public GroovyLexer(String source) {
    this.source = source;
    this.position = 0;
    this.line = 1;
    this.column = 1;
  }

  /** ソースコード全体を字句解析してトークンリストを返す */
  public Either<String, List<Token>> tokenize() {
    try {
      List<Token> tokens = List.empty();

      while (!isAtEnd()) {
        Token token = nextToken();
        if (token.type() != TokenType.WHITESPACE) {
          tokens = tokens.append(token);
        }
      }

      // EOFトークンを追加
      tokens = tokens.append(new Token(TokenType.EOF, "", position, position, line, column));

      return Either.right(tokens);
    } catch (StringIndexOutOfBoundsException e) {
      return Either.left("字句解析エラー: 予期しない入力終了 - 位置: " + position);
    } catch (Exception e) {
      return Either.left("字句解析エラー: " + e.getMessage());
    }
  }

  /** 次のトークンを取得 */
  private Token nextToken() {
    skipWhitespace();

    if (isAtEnd()) {
      return new Token(TokenType.EOF, "", position, position, line, column);
    }

    int startPosition = position;
    int startLine = line;
    int startColumn = column;

    char c = advance();

    // 識別子またはキーワード
    if (isAlpha(c) || c == '_') {
      return scanIdentifierOrKeyword(startPosition, startLine, startColumn);
    }

    // 数値リテラル
    if (isDigit(c)) {
      return scanNumber(startPosition, startLine, startColumn);
    }

    // 文字列リテラル
    if (c == '"' || c == '\'') {
      return scanString(c, startPosition, startLine, startColumn);
    }

    // 演算子と区切り文字
    switch (c) {
      case '+':
        return new Token(TokenType.PLUS, "+", startPosition, position, startLine, startColumn);
      case '-':
        return new Token(TokenType.MINUS, "-", startPosition, position, startLine, startColumn);
      case '*':
        return new Token(TokenType.MULTIPLY, "*", startPosition, position, startLine, startColumn);
      case '/':
        // コメントチェック
        if (peek() == '/') {
          return scanLineComment(startPosition, startLine, startColumn);
        } else if (peek() == '*') {
          return scanBlockComment(startPosition, startLine, startColumn);
        }
        return new Token(TokenType.DIVIDE, "/", startPosition, position, startLine, startColumn);
      case '%':
        return new Token(TokenType.MODULO, "%", startPosition, position, startLine, startColumn);
      case '=':
        if (peek() == '=') {
          advance();
          return new Token(TokenType.EQUALS, "==", startPosition, position, startLine, startColumn);
        }
        return new Token(TokenType.ASSIGN, "=", startPosition, position, startLine, startColumn);
      case '!':
        if (peek() == '=') {
          advance();
          return new Token(
              TokenType.NOT_EQUALS, "!=", startPosition, position, startLine, startColumn);
        }
        return new Token(TokenType.NOT, "!", startPosition, position, startLine, startColumn);
      case '<':
        if (peek() == '=') {
          advance();
          return new Token(
              TokenType.LESS_THAN_OR_EQUAL, "<=", startPosition, position, startLine, startColumn);
        }
        return new Token(TokenType.LESS_THAN, "<", startPosition, position, startLine, startColumn);
      case '>':
        if (peek() == '=') {
          advance();
          return new Token(
              TokenType.GREATER_THAN_OR_EQUAL,
              ">=",
              startPosition,
              position,
              startLine,
              startColumn);
        }
        return new Token(
            TokenType.GREATER_THAN, ">", startPosition, position, startLine, startColumn);
      case '&':
        if (peek() == '&') {
          advance();
          return new Token(TokenType.AND, "&&", startPosition, position, startLine, startColumn);
        }
        break;
      case '|':
        if (peek() == '|') {
          advance();
          return new Token(TokenType.OR, "||", startPosition, position, startLine, startColumn);
        }
        break;
      case '(':
        return new Token(
            TokenType.LEFT_PAREN, "(", startPosition, position, startLine, startColumn);
      case ')':
        return new Token(
            TokenType.RIGHT_PAREN, ")", startPosition, position, startLine, startColumn);
      case '{':
        return new Token(
            TokenType.LEFT_BRACE, "{", startPosition, position, startLine, startColumn);
      case '}':
        return new Token(
            TokenType.RIGHT_BRACE, "}", startPosition, position, startLine, startColumn);
      case '[':
        return new Token(
            TokenType.LEFT_BRACKET, "[", startPosition, position, startLine, startColumn);
      case ']':
        return new Token(
            TokenType.RIGHT_BRACKET, "]", startPosition, position, startLine, startColumn);
      case ';':
        return new Token(TokenType.SEMICOLON, ";", startPosition, position, startLine, startColumn);
      case ',':
        return new Token(TokenType.COMMA, ",", startPosition, position, startLine, startColumn);
      case '.':
        return new Token(TokenType.DOT, ".", startPosition, position, startLine, startColumn);
      case ':':
        return new Token(TokenType.COLON, ":", startPosition, position, startLine, startColumn);
      default: // fall out
    }

    // 不明な文字
    return new Token(
        TokenType.UNKNOWN, String.valueOf(c), startPosition, position, startLine, startColumn);
  }

  /** 識別子またはキーワードをスキャン */
  private Token scanIdentifierOrKeyword(int startPosition, int startLine, int startColumn) {
    while (isAlphaNumeric(peek()) || peek() == '_') {
      advance();
    }

    String text = source.substring(startPosition, position);
    TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);

    return new Token(type, text, startPosition, position, startLine, startColumn);
  }

  /** 数値リテラルをスキャン */
  private Token scanNumber(int startPosition, int startLine, int startColumn) {
    while (isDigit(peek())) {
      advance();
    }

    // 小数点のチェック
    if (peek() == '.' && isDigit(peekNext())) {
      advance(); // '.'を消費
      while (isDigit(peek())) {
        advance();
      }
    }

    String text = source.substring(startPosition, position);
    return new Token(
        TokenType.NUMBER_LITERAL, text, startPosition, position, startLine, startColumn);
  }

  /** 文字列リテラルをスキャン */
  private Token scanString(char quote, int startPosition, int startLine, int startColumn) {
    while (peek() != quote && !isAtEnd()) {
      if (peek() == '\n') {
        line++;
        column = 1;
      }
      advance();
    }

    if (isAtEnd()) {
      // エラー: 閉じられていない文字列
      String text = source.substring(startPosition, position);
      return new Token(
          TokenType.ERROR_UNCLOSED_STRING, text, startPosition, position, startLine, startColumn);
    }

    // 閉じクォートを消費
    advance();

    String text = source.substring(startPosition, position);
    return new Token(
        TokenType.STRING_LITERAL, text, startPosition, position, startLine, startColumn);
  }

  /** 行コメントをスキャン */
  private Token scanLineComment(int startPosition, int startLine, int startColumn) {
    // "//"を消費
    advance();

    while (peek() != '\n' && !isAtEnd()) {
      advance();
    }

    String text = source.substring(startPosition, position);
    return new Token(TokenType.COMMENT, text, startPosition, position, startLine, startColumn);
  }

  /** ブロックコメントをスキャン */
  private Token scanBlockComment(int startPosition, int startLine, int startColumn) {
    // "/*"を消費
    advance();

    while (!isAtEnd()) {
      if (peek() == '*' && peekNext() == '/') {
        advance(); // '*'を消費
        advance(); // '/'を消費
        break;
      }
      if (peek() == '\n') {
        line++;
        column = 1;
      }
      advance();
    }

    String text = source.substring(startPosition, position);
    // 閉じられていないブロックコメントのチェック
    if (isAtEnd() && !text.endsWith("*/")) {
      return new Token(
          TokenType.ERROR_UNCLOSED_COMMENT, text, startPosition, position, startLine, startColumn);
    }
    return new Token(TokenType.COMMENT, text, startPosition, position, startLine, startColumn);
  }

  /** 空白文字をスキップ */
  private void skipWhitespace() {

    while (!isAtEnd()) {
      char c = peek();
      switch (c) {
        case ' ':
        case '\r':
        case '\t':
          advance();
          break;
        case '\n':
          line++;
          column = 1;
          position++;
          break;
        default:
          return;
      }
    }
  }

  private boolean isAtEnd() {
    return position >= source.length();
  }

  private char advance() {
    char c = source.charAt(position);
    position++;
    column++;
    return c;
  }

  private char peek() {
    if (isAtEnd()) {
      return '\0';
    }
    return source.charAt(position);
  }

  private char peekNext() {
    if (position + 1 >= source.length()) {
      return '\0';
    }
    return source.charAt(position + 1);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }
}
