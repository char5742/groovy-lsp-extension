package com.groovylsp.domain.model;

import java.util.List;

/**
 * クラス情報を表すドメインモデル
 *
 * <p>GroovyソースコードのASTから抽出されたクラス定義の情報を保持します。 LSPの各機能（定義ジャンプ、補完、ホバー等）で使用されます。
 */
public record ClassInfo(
    String name,
    String qualifiedName,
    ClassType type,
    Position position,
    List<MethodInfo> methods,
    List<FieldInfo> fields,
    List<String> superTypes,
    List<String> interfaces,
    int modifiers) {

  /** クラスの種別 */
  public enum ClassType {
    /** 通常のクラス */
    CLASS,
    /** インターフェース */
    INTERFACE,
    /** 列挙型 */
    ENUM,
    /** アノテーション */
    ANNOTATION,
    /** トレイト */
    TRAIT,
    /** Groovyスクリプト */
    SCRIPT
  }

  /** パブリッククラスかどうかを判定 */
  public boolean isPublic() {
    return java.lang.reflect.Modifier.isPublic(modifiers);
  }

  /** 抽象クラスかどうかを判定 */
  public boolean isAbstract() {
    return java.lang.reflect.Modifier.isAbstract(modifiers);
  }

  /** ファイナルクラスかどうかを判定 */
  public boolean isFinal() {
    return java.lang.reflect.Modifier.isFinal(modifiers);
  }

  /**
   * 位置情報（ソースコード内の位置）
   *
   * @param startLine 開始行（1ベース）
   * @param startColumn 開始列（1ベース）
   * @param endLine 終了行（1ベース）
   * @param endColumn 終了列（1ベース）
   */
  public record Position(int startLine, int startColumn, int endLine, int endColumn) {
    /** LSP準拠の位置情報（0ベース）に変換 */
    public DiagnosticItem.DocumentPosition toStartLspPosition() {
      return new DiagnosticItem.DocumentPosition(startLine - 1, startColumn - 1);
    }

    /** LSP準拠の終了位置情報（0ベース）に変換 */
    public DiagnosticItem.DocumentPosition toEndLspPosition() {
      return new DiagnosticItem.DocumentPosition(endLine - 1, endColumn - 1);
    }
  }
}
