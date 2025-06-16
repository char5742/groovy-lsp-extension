package com.groovylsp.domain.lexer;

/** Groovyトークンタイプの定義 */
public enum TokenType {
  // キーワード
  DEF("def"),
  CLASS("class"),
  INTERFACE("interface"),
  ENUM("enum"),
  IF("if"),
  ELSE("else"),
  WHILE("while"),
  FOR("for"),
  RETURN("return"),
  NEW("new"),
  TRY("try"),
  CATCH("catch"),
  FINALLY("finally"),
  THROW("throw"),
  THIS("this"),
  SUPER("super"),
  NULL("null"),
  TRUE("true"),
  FALSE("false"),
  IMPORT("import"),
  PACKAGE("package"),
  EXTENDS("extends"),
  IMPLEMENTS("implements"),
  PUBLIC("public"),
  PRIVATE("private"),
  PROTECTED("protected"),
  STATIC("static"),
  FINAL("final"),
  ABSTRACT("abstract"),
  VOID("void"),
  IN("in"),
  AS("as"),
  ASSERT("assert"),
  BREAK("break"),
  CASE("case"),
  CONTINUE("continue"),
  DEFAULT("default"),
  DO("do"),
  INSTANCEOF("instanceof"),
  SWITCH("switch"),

  // リテラル
  STRING_LITERAL,
  NUMBER_LITERAL,

  // 識別子
  IDENTIFIER,

  // 演算子
  PLUS,
  MINUS,
  MULTIPLY,
  DIVIDE,
  MODULO,
  ASSIGN,
  EQUALS,
  NOT_EQUALS,
  LESS_THAN,
  GREATER_THAN,
  LESS_THAN_OR_EQUAL,
  GREATER_THAN_OR_EQUAL,
  AND,
  OR,
  NOT,

  // 区切り文字
  LEFT_PAREN,
  RIGHT_PAREN,
  LEFT_BRACE,
  RIGHT_BRACE,
  LEFT_BRACKET,
  RIGHT_BRACKET,
  SEMICOLON,
  COMMA,
  DOT,
  COLON,

  // その他
  WHITESPACE,
  NEWLINE,
  COMMENT,
  EOF,
  UNKNOWN,

  // エラートークン
  ERROR_UNCLOSED_STRING,
  ERROR_UNCLOSED_COMMENT,
  ERROR_INVALID_NUMBER;

  private final String keyword;

  TokenType() {
    this.keyword = null;
  }

  TokenType(String keyword) {
    this.keyword = keyword;
  }

  public String getKeyword() {
    return keyword;
  }

  public boolean isKeyword() {
    return keyword != null;
  }
}
