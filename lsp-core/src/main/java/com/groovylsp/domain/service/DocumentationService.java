package com.groovylsp.domain.service;

import com.groovylsp.domain.model.Documentation;
import io.vavr.control.Option;
import org.codehaus.groovy.ast.ASTNode;

/**
 * ドキュメント抽出サービス
 *
 * <p>JavaDoc/GroovyDocコメントを抽出し、構造化されたドキュメント情報を提供します。
 */
public interface DocumentationService {

  /**
   * ASTノードからドキュメントを取得
   *
   * @param node ASTノード
   * @return ドキュメント情報、見つからない場合は空のOption
   */
  Option<Documentation> getDocumentation(ASTNode node);

  /**
   * 完全修飾名から外部ライブラリのドキュメントを取得
   *
   * @param fullyQualifiedName 完全修飾名
   * @return ドキュメント情報、見つからない場合は空のOption
   */
  Option<Documentation> getExternalDocumentation(String fullyQualifiedName);

  /**
   * ドキュメントをMarkdown形式に変換
   *
   * @param documentation ドキュメント情報
   * @return Markdown形式の文字列
   */
  String formatDocumentation(Documentation documentation);

  /**
   * ドキュメントコメント文字列を解析
   *
   * @param comment ドキュメントコメント文字列
   * @return 解析されたドキュメント情報
   */
  Documentation parseDocumentationComment(String comment);

  /**
   * ソースコード内容からドキュメントコメントを抽出
   *
   * @param node ASTノード
   * @param sourceContent ソースファイルの内容
   * @return ドキュメントコメント
   */
  Option<String> extractDocCommentFromSource(ASTNode node, String sourceContent);
}