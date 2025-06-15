package com.groovylsp.domain.service;

import com.groovylsp.domain.model.LineCountResult;
import io.vavr.control.Either;
import java.util.Objects;

/** 行カウント機能を提供するドメインサービス。 */
public class LineCountService {

  /**
   * 指定されたコンテンツの行数をカウントする。
   *
   * @param content カウント対象のコンテンツ
   * @return 行カウント結果またはエラー
   */
  public Either<String, LineCountResult> countLines(String content) {
    Objects.requireNonNull(content, "content must not be null");

    if (content.isEmpty()) {
      return Either.right(LineCountResult.empty());
    }

    String[] lines = content.split("\\r?\\n", -1);
    int totalLines = lines.length;
    int blankLines = 0;
    int commentLines = 0;

    for (String line : lines) {
      String trimmedLine = line.trim();

      if (trimmedLine.isEmpty()) {
        blankLines++;
      } else if (isCommentLine(trimmedLine)) {
        commentLines++;
      }
    }

    int codeLines = totalLines - blankLines - commentLines;

    return Either.right(new LineCountResult(totalLines, blankLines, commentLines, codeLines));
  }

  /**
   * 指定された行がコメント行かどうかを判定する。 Groovyの単一行コメントと複数行コメントの開始を検出する。
   *
   * @param trimmedLine トリムされた行
   * @return コメント行の場合true
   */
  private boolean isCommentLine(String trimmedLine) {
    return trimmedLine.startsWith("//")
        || trimmedLine.startsWith("/*")
        || trimmedLine.startsWith("*")
        || trimmedLine.startsWith("*/");
  }
}
