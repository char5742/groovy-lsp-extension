package com.groovylsp.domain.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * AST解析結果を表すドメインモデル
 *
 * <p>GroovyソースコードのAST解析結果をまとめて保持します。 LSPの各機能で共通的に使用されます。
 */
public record AstInfo(
    String uri,
    List<ClassInfo> classes,
    List<DiagnosticItem> syntaxErrors,
    String packageName,
    List<ImportInfo> imports) {

  /** クラスが定義されているかどうかを判定 */
  public boolean hasClasses() {
    return !classes.isEmpty();
  }

  /** 構文エラーがあるかどうかを判定 */
  public boolean hasSyntaxErrors() {
    return !syntaxErrors.isEmpty();
  }

  /**
   * 指定した名前のクラスを検索
   *
   * @param className クラス名
   * @return 見つかったクラス情報、見つからない場合はnull
   */
  public ClassInfo findClassByName(String className) {
    return classes.stream().filter(c -> c.name().equals(className)).findFirst().orElse(null);
  }

  /** すべてのメソッド情報を取得 */
  public List<MethodInfo> getAllMethods() {
    return classes.stream().flatMap(c -> c.methods().stream()).toList();
  }

  /** すべてのフィールド情報を取得 */
  public List<FieldInfo> getAllFields() {
    return classes.stream().flatMap(c -> c.fields().stream()).toList();
  }

  /**
   * エイリアス名から元のクラス名を解決
   *
   * @param aliasName エイリアス名
   * @return 元のクラス名、見つからない場合はnull
   */
  public @Nullable String resolveAlias(String aliasName) {
    return imports.stream()
        .filter(imp -> aliasName.equals(imp.alias()))
        .map(ImportInfo::className)
        .findFirst()
        .orElse(null);
  }

  /**
   * import文のエイリアスマップを取得
   *
   * @return エイリアス名 -> 完全修飾クラス名のマップ
   */
  public Map<String, String> getAliasMap() {
    return imports.stream()
        .filter(imp -> imp.alias() != null)
        .collect(Collectors.toMap(ImportInfo::alias, ImportInfo::className));
  }

  /**
   * クラス名（単純名またはエイリアス）からimport情報を検索
   *
   * @param name クラス名またはエイリアス
   * @return import情報、見つからない場合はnull
   */
  public @Nullable ImportInfo findImportByNameOrAlias(String name) {
    return imports.stream()
        .filter(imp -> name.equals(imp.usageName()) || name.equals(imp.alias()))
        .findFirst()
        .orElse(null);
  }
}
