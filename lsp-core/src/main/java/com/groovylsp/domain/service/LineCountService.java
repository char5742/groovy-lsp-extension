package com.groovylsp.domain.service;

import com.groovylsp.domain.model.LineCountResult;
import io.vavr.control.Either;

/**
 * 行カウント機能を提供するドメインサービス。
 *
 * <p>このサービスはGroovyファイルの行数をカウントし、コード行、コメント行、空行に分類します。
 *
 * <h3>現在の制限事項:</h3>
 *
 * <ul>
 *   <li>文字列リテラル内のコメント記号は考慮しません
 *   <li>インラインコメントを含む行は全体をコメント行として扱います
 *   <li>ネストした複数行コメントはサポートしません
 * </ul>
 *
 * <h3>エラーハンドリング:</h3>
 *
 * <p>現在の実装ではエラーが発生するケースはありませんが、将来の拡張性を考慮して Either型を返しています。将来的には以下のようなエラーが想定されます：
 *
 * <ul>
 *   <li>文法解析エラー（より高度な解析を行う場合）
 *   <li>ファイルサイズ制限エラー
 *   <li>エンコーディングエラー
 * </ul>
 */
public class LineCountService {

  /**
   * 指定されたコンテンツの行数をカウントする。
   *
   * @param content カウント対象のコンテンツ（null不可）
   * @return 行カウント結果を含むEither。現在の実装では常にRightを返す
   * @throws NullPointerException contentがnullの場合
   */
  public Either<String, LineCountResult> countLines(String content) {

    if (content.isEmpty()) {
      return Either.right(LineCountResult.empty());
    }

    String[] lines = content.split("\\r?\\n", -1);
    int totalLines = lines.length;
    int blankLines = 0;
    int commentLines = 0;
    boolean inMultiLineComment = false;

    for (String line : lines) {
      String trimmedLine = line.trim();

      if (trimmedLine.isEmpty()) {
        blankLines++;
      } else {
        var result = analyzeLineForComment(trimmedLine, inMultiLineComment);
        if (result.isComment) {
          commentLines++;
        }
        inMultiLineComment = result.inMultiLineComment;
      }
    }

    int codeLines = totalLines - blankLines - commentLines;

    return Either.right(new LineCountResult(totalLines, blankLines, commentLines, codeLines));
  }

  /**
   * 行をコメントかどうか分析する。
   *
   * @param line 分析対象の行（トリム済み）
   * @param wasInMultiLineComment 前の行が複数行コメント内だったか
   * @return 分析結果
   */
  private CommentAnalysisResult analyzeLineForComment(String line, boolean wasInMultiLineComment) {
    // 単一行コメント
    if (line.startsWith("//")) {
      return new CommentAnalysisResult(true, false);
    }

    // 複数行コメントの処理
    if (wasInMultiLineComment) {
      // 複数行コメント内
      if (line.contains("*/")) {
        // コメント終了を含む行
        // 終了後にコードがある可能性もあるが、簡易実装としてコメント行とする
        return new CommentAnalysisResult(true, false);
      } else {
        // まだコメント内
        return new CommentAnalysisResult(true, true);
      }
    } else {
      // 複数行コメント外
      if (line.startsWith("/*")) {
        if (line.contains("*/") && line.indexOf("*/") > line.indexOf("/*")) {
          // 同一行で開始・終了
          return new CommentAnalysisResult(true, false);
        } else {
          // 複数行コメント開始
          return new CommentAnalysisResult(true, true);
        }
      }
    }

    // コード行
    return new CommentAnalysisResult(false, false);
  }

  /** コメント分析結果を表す内部クラス。 */
  private static class CommentAnalysisResult {
    final boolean isComment;
    final boolean inMultiLineComment;

    CommentAnalysisResult(boolean isComment, boolean inMultiLineComment) {
      this.isComment = isComment;
      this.inMultiLineComment = inMultiLineComment;
    }
  }
}
