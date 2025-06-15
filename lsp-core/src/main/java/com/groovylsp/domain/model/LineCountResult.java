package com.groovylsp.domain.model;

/** ファイルの行カウント結果を表すドメインモデル。 */
public record LineCountResult(int totalLines, int blankLines, int commentLines, int codeLines) {

  public LineCountResult {
    if (totalLines < 0) {
      throw new IllegalArgumentException("総行数は負の値になりません: " + totalLines);
    }
    if (blankLines < 0) {
      throw new IllegalArgumentException("空行数は負の値になりません: " + blankLines);
    }
    if (commentLines < 0) {
      throw new IllegalArgumentException("コメント行数は負の値になりません: " + commentLines);
    }
    if (codeLines < 0) {
      throw new IllegalArgumentException("コード行数は負の値になりません: " + codeLines);
    }
    if (totalLines != blankLines + commentLines + codeLines) {
      throw new IllegalArgumentException(
          String.format(
              "行数の合計が一致しません: 総行数=%d, 空行=%d, コメント=%d, コード=%d",
              totalLines, blankLines, commentLines, codeLines));
    }
  }

  /** 空の行カウント結果を作成する。 */
  public static LineCountResult empty() {
    return new LineCountResult(0, 0, 0, 0);
  }

  /** 行カウント結果をフォーマットした文字列を返す。 */
  public String toFormattedString() {
    return String.format(
        "総行数: %d行 (コード: %d行, 空行: %d行, コメント: %d行)", totalLines, codeLines, blankLines, commentLines);
  }
}
