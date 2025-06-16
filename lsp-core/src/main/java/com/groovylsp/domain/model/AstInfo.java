package com.groovylsp.domain.model;

import java.util.List;

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
    List<String> imports) {

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
}
