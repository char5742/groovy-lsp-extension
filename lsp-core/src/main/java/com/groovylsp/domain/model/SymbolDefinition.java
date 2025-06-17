package com.groovylsp.domain.model;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;

/**
 * シンボルの定義情報を表すドメインモデル
 *
 * <p>シンボルの名前、種類、定義位置、所属するスコープなどの情報を保持します。
 * textDocument/definitionやtextDocument/referencesなどの機能で使用されます。
 */
public record SymbolDefinition(
    String name,
    String qualifiedName,
    SymbolKind kind,
    String uri,
    Range range,
    Range selectionRange,
    String containingClass,
    DefinitionType definitionType) {

  /** 定義の種類 */
  public enum DefinitionType {
    /** クラス定義 */
    CLASS,
    /** メソッド定義 */
    METHOD,
    /** フィールド定義 */
    FIELD,
    /** ローカル変数定義 */
    LOCAL_VARIABLE,
    /** パラメータ定義 */
    PARAMETER,
    /** インポート定義 */
    IMPORT
  }

  /**
   * LSPのLocation形式に変換
   *
   * @return Location オブジェクト
   */
  public Location toLocation() {
    return new Location(uri, range);
  }

  /**
   * 選択範囲を持つLocationに変換
   *
   * @return Location オブジェクト（選択範囲を使用）
   */
  public Location toSelectionLocation() {
    return new Location(uri, selectionRange);
  }

  /** クラス定義を作成 */
  public static SymbolDefinition forClass(
      ClassInfo classInfo, String uri, Range range, Range selectionRange) {
    return new SymbolDefinition(
        classInfo.name(),
        classInfo.qualifiedName(),
        switch (classInfo.type()) {
          case CLASS, SCRIPT -> SymbolKind.Class;
          case INTERFACE -> SymbolKind.Interface;
          case ENUM -> SymbolKind.Enum;
          case ANNOTATION -> SymbolKind.Struct;
          case TRAIT -> SymbolKind.Class;
        },
        uri,
        range,
        selectionRange,
        null,
        DefinitionType.CLASS);
  }

  /** メソッド定義を作成 */
  public static SymbolDefinition forMethod(
      MethodInfo methodInfo,
      String qualifiedClassName,
      String uri,
      Range range,
      Range selectionRange) {
    return new SymbolDefinition(
        methodInfo.name(),
        qualifiedClassName + "." + methodInfo.name(),
        SymbolKind.Method,
        uri,
        range,
        selectionRange,
        qualifiedClassName,
        DefinitionType.METHOD);
  }

  /** フィールド定義を作成 */
  public static SymbolDefinition forField(
      FieldInfo fieldInfo,
      String qualifiedClassName,
      String uri,
      Range range,
      Range selectionRange) {
    return new SymbolDefinition(
        fieldInfo.name(),
        qualifiedClassName + "." + fieldInfo.name(),
        fieldInfo.isStatic() ? SymbolKind.Constant : SymbolKind.Field,
        uri,
        range,
        selectionRange,
        qualifiedClassName,
        DefinitionType.FIELD);
  }
}
