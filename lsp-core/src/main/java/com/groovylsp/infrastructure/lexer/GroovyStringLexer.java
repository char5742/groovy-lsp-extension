package com.groovylsp.infrastructure.lexer;

import com.groovylsp.domain.lexer.Token;
import com.groovylsp.domain.lexer.TokenType;

/**
 * Groovy固有の文字列リテラル処理を行うヘルパークラス
 *
 * <p>今後の拡張に備えて： - GString（"Hello ${name}"）のサポート - トリプルクォート文字列（'''...''' や """..."""） -
 * スラッシュ文字列（/pattern/）
 */
public class GroovyStringLexer {

  private final String source;
  private final int startPosition;
  private final int startLine;
  private final int startColumn;

  public GroovyStringLexer(String source, int startPosition, int startLine, int startColumn) {
    this.source = source;
    this.startPosition = startPosition;
    this.startLine = startLine;
    this.startColumn = startColumn;
  }

  /** GString内の式を解析する準備 現在は通常の文字列として扱うが、将来的には${expression}を個別のトークンに分解 */
  public Token scanGString(int currentPosition) {
    // TODO: GString実装時に${expression}の解析を追加
    // 現在は通常の文字列リテラルとして扱う
    String text = source.substring(startPosition, currentPosition);
    return new Token(
        TokenType.STRING_LITERAL, text, startPosition, currentPosition, startLine, startColumn);
  }

  /** トリプルクォート文字列の解析準備 */
  public boolean isTripleQuote(int position) {
    if (position + 2 >= source.length()) {
      return false;
    }
    char c = source.charAt(position);
    return (c == '\'' || c == '"')
        && source.charAt(position + 1) == c
        && source.charAt(position + 2) == c;
  }
}
