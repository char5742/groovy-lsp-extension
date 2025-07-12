package com.groovylsp.infrastructure.documentation;

import com.groovylsp.domain.service.DocumentationService;
import io.vavr.control.Option;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ソースコードからドキュメントコメントを抽出するサービス
 *
 * <p>ASTの位置情報を使用してソースファイルから直接ドキュメントコメントを抽出します。
 */
@Singleton
public class SourceDocumentExtractor {

  private static final Logger logger = LoggerFactory.getLogger(SourceDocumentExtractor.class);

  

  @Inject
  public SourceDocumentExtractor(DocumentationService documentationService) {
    
  }

  /**
   * ソースファイルからASTノードの直前のドキュメントコメントを抽出
   *
   * @param node ASTノード
   * @param sourceFilePath ソースファイルのパス
   * @return ドキュメントコメント、見つからない場合は空のOption
   */
  public Option<String> extractDocComment(ASTNode node, String sourceFilePath) {
    try {
      var path = Path.of(sourceFilePath);
      if (!Files.exists(path)) {
        logger.debug("ソースファイルが見つかりません: {}", sourceFilePath);
        return Option.none();
      }

      List<String> lines = Files.readAllLines(path);
      return extractDocCommentFromLines(node, lines);

    } catch (IOException e) {
      logger.warn("ソースファイルの読み込みに失敗: {}", sourceFilePath, e);
      return Option.none();
    }
  }

  /**
   * ソースコードの行からASTノードの直前のドキュメントコメントを抽出
   *
   * @param node ASTノード
   * @param lines ソースコードの行リスト
   * @return ドキュメントコメント、見つからない場合は空のOption
   */
  public Option<String> extractDocCommentFromLines(ASTNode node, List<String> lines) {
    if (node.getLineNumber() < 1 || lines.isEmpty()) {
      return Option.none();
    }

    // ASTの行番号は1ベース、リストのインデックスは0ベースなので-1
    int nodeLineIndex = node.getLineNumber() - 1;
    
    // ノードの直前の行から逆順でドキュメントコメントを探す
    var commentLines = new ArrayList<String>();
    boolean inComment = false;
    
    for (int i = nodeLineIndex - 1; i >= 0; i--) {
      String line = lines.get(i).trim();
      
      if (line.isEmpty()) {
        // 空行はコメントブロック内でない限りスキップ
        if (!inComment) {
          continue;
        }
      } else if (line.endsWith("*/")) {
        // コメント終了を発見
        if (line.contains("/**") || line.contains("/*")) {
          // 単一行コメント
          commentLines.add(0, line);
          inComment = false;
          break;
        } else {
          // 複数行コメントの終了
          commentLines.add(0, line);
          inComment = true;
        }
      } else if (inComment && (line.startsWith("*") || line.startsWith("/*"))) {
        // コメント内の行
        commentLines.add(0, line);
        if (line.startsWith("/**") || line.startsWith("/*")) {
          // コメント開始を発見
          break;
        }
      } else if (line.startsWith("/**")) {
        // JavaDoc/GroovyDocコメント開始
        commentLines.add(0, line);
        if (line.endsWith("*/")) {
          // 単一行コメント
          break;
        } else {
          inComment = true;
        }
      } else if (!inComment) {
        // コメント以外のコードに到達したらドキュメントコメントはない
        break;
      }
    }
    
    if (commentLines.isEmpty()) {
      return Option.none();
    }
    
    // ドキュメントコメントかどうかチェック（/** で始まる）
    String firstLine = commentLines.get(0).trim();
    if (!firstLine.startsWith("/**")) {
      return Option.none();
    }
    
    return Option.of(String.join("\n", commentLines));
  }

  /**
   * ソースコード内の位置からドキュメントコメントを抽出
   *
   * @param sourceCode ソースコード全体
   * @param lineNumber 行番号（1ベース）
   * @return ドキュメントコメント、見つからない場合は空のOption
   */
  public Option<String> extractDocCommentFromSource(String sourceCode, int lineNumber) {
    var lines = List.of(sourceCode.split("\n"));
    
    // 仮のASTノードを作成（行番号のみ設定）
    var fakeNode = new ASTNode() {
      @Override
      public int getLineNumber() {
        return lineNumber;
      }
    };
    
    return extractDocCommentFromLines(fakeNode, lines);
  }
}