package com.groovylsp.domain.lexer;

/** 字句解析で生成されるトークン */
public record Token(
    TokenType type, String text, int startPosition, int endPosition, int line, int column) {
  public Token {
    if (type == null) {
      throw new IllegalArgumentException("トークンタイプはnullにできません");
    }
    if (text == null) {
      throw new IllegalArgumentException("トークンテキストはnullにできません");
    }
    if (startPosition < 0) {
      throw new IllegalArgumentException("開始位置は負の値にできません");
    }
    if (endPosition < startPosition) {
      throw new IllegalArgumentException("終了位置は開始位置より前にできません");
    }
    if (line < 1) {
      throw new IllegalArgumentException("行番号は1以上である必要があります");
    }
    if (column < 1) {
      throw new IllegalArgumentException("列番号は1以上である必要があります");
    }
  }

  /** トークンの長さを取得 */
  public int length() {
    return endPosition - startPosition;
  }

  /** トークンがキーワードかどうかを判定 */
  public boolean isKeyword() {
    return type.isKeyword();
  }

  /** トークンが指定されたタイプかどうかを判定 */
  public boolean is(TokenType expectedType) {
    return type == expectedType;
  }

  /** トークンが指定されたタイプのいずれかかどうかを判定 */
  public boolean isAnyOf(TokenType... expectedTypes) {
    for (TokenType expectedType : expectedTypes) {
      if (type == expectedType) {
        return true;
      }
    }
    return false;
  }
}
