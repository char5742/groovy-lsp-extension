package com.groovylsp.domain.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * メソッド情報を表すドメインモデル
 *
 * <p>GroovyソースコードのASTから抽出されたメソッド定義の情報を保持します。 LSPの各機能（定義ジャンプ、補完、シグネチャヘルプ等）で使用されます。
 */
public record MethodInfo(
    String name,
    String returnType,
    List<ParameterInfo> parameters,
    ClassInfo.Position position,
    int modifiers,
    @Nullable String documentation) {

  /** パブリックメソッドかどうかを判定 */
  public boolean isPublic() {
    return java.lang.reflect.Modifier.isPublic(modifiers);
  }

  /** プライベートメソッドかどうかを判定 */
  public boolean isPrivate() {
    return java.lang.reflect.Modifier.isPrivate(modifiers);
  }

  /** 保護メソッドかどうかを判定 */
  public boolean isProtected() {
    return java.lang.reflect.Modifier.isProtected(modifiers);
  }

  /** 静的メソッドかどうかを判定 */
  public boolean isStatic() {
    return java.lang.reflect.Modifier.isStatic(modifiers);
  }

  /** 抽象メソッドかどうかを判定 */
  public boolean isAbstract() {
    return java.lang.reflect.Modifier.isAbstract(modifiers);
  }

  /** ファイナルメソッドかどうかを判定 */
  public boolean isFinal() {
    return java.lang.reflect.Modifier.isFinal(modifiers);
  }

  /** メソッドのシグネチャを文字列として取得 */
  public String getSignature() {
    var params =
        parameters.stream()
            .map(p -> p.type() + " " + p.name())
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    return name + "(" + params + ")";
  }

  /** メソッドパラメータ情報 */
  public record ParameterInfo(
      String name, String type, @Nullable String defaultValue, boolean isVarArgs) {}
}
