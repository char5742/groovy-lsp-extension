package com.groovylsp.domain.service;

import io.vavr.control.Option;
import org.jspecify.annotations.Nullable;

/**
 * JavaDocドキュメント取得サービスのインターフェース
 *
 * <p>JDKクラスや外部ライブラリのJavaDocドキュメントを取得します。 ホバー機能でJavaクラスの詳細なドキュメンテーションを提供するために使用されます。
 */
public interface JavaDocService {

  /**
   * 指定されたクラスのJavaDocドキュメントを取得
   *
   * @param className 完全修飾クラス名（例: "java.lang.String"）
   * @return JavaDocドキュメント、見つからない場合はOption.none()
   */
  Option<JavaDocInfo> getClassDocumentation(String className);

  /**
   * 指定されたメソッドのJavaDocドキュメントを取得
   *
   * @param className 完全修飾クラス名
   * @param methodName メソッド名
   * @param parameterTypes パラメータの型名リスト
   * @return JavaDocドキュメント、見つからない場合はOption.none()
   */
  Option<JavaDocInfo> getMethodDocumentation(
      String className, String methodName, String[] parameterTypes);

  /**
   * 指定されたフィールドのJavaDocドキュメントを取得
   *
   * @param className 完全修飾クラス名
   * @param fieldName フィールド名
   * @return JavaDocドキュメント、見つからない場合はOption.none()
   */
  Option<JavaDocInfo> getFieldDocumentation(String className, String fieldName);

  /** JavaDocドキュメント情報 */
  record JavaDocInfo(
      String summary,
      @Nullable String description,
      @Nullable String parameters,
      @Nullable String returnValue,
      @Nullable String exceptions,
      @Nullable String since,
      @Nullable String deprecated,
      @Nullable String see) {

    /**
     * JavaDocInfoをMarkdown形式でフォーマット
     *
     * @return フォーマットされたMarkdown文字列
     */
    public String toMarkdown() {
      var sb = new StringBuilder();

      if (summary != null && !summary.trim().isEmpty()) {
        sb.append(summary).append("\n\n");
      }

      if (description != null && !description.trim().isEmpty()) {
        sb.append(description).append("\n\n");
      }

      if (deprecated != null && !deprecated.trim().isEmpty()) {
        sb.append("**⚠️ 非推奨**: ").append(deprecated).append("\n\n");
      }

      if (parameters != null && !parameters.trim().isEmpty()) {
        sb.append("**パラメータ**:\n").append(parameters).append("\n\n");
      }

      if (returnValue != null && !returnValue.trim().isEmpty()) {
        sb.append("**戻り値**: ").append(returnValue).append("\n\n");
      }

      if (exceptions != null && !exceptions.trim().isEmpty()) {
        sb.append("**例外**:\n").append(exceptions).append("\n\n");
      }

      if (since != null && !since.trim().isEmpty()) {
        sb.append("**バージョン**: ").append(since).append("\n\n");
      }

      if (see != null && !see.trim().isEmpty()) {
        sb.append("**関連項目**:\n").append(see).append("\n\n");
      }

      return sb.toString().trim();
    }
  }
}
