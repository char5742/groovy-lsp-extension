package com.groovylsp.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * フィールド情報を表すドメインモデル
 *
 * <p>GroovyソースコードのASTから抽出されたフィールド定義の情報を保持します。 LSPの各機能（定義ジャンプ、補完、ホバー等）で使用されます。
 */
public record FieldInfo(
    String name,
    String type,
    ClassInfo.Position position,
    int modifiers,
    @Nullable String initialValue,
    @Nullable String documentation) {

  /** パブリックフィールドかどうかを判定 */
  public boolean isPublic() {
    return java.lang.reflect.Modifier.isPublic(modifiers);
  }

  /** プライベートフィールドかどうかを判定 */
  public boolean isPrivate() {
    return java.lang.reflect.Modifier.isPrivate(modifiers);
  }

  /** 保護フィールドかどうかを判定 */
  public boolean isProtected() {
    return java.lang.reflect.Modifier.isProtected(modifiers);
  }

  /** 静的フィールドかどうかを判定 */
  public boolean isStatic() {
    return java.lang.reflect.Modifier.isStatic(modifiers);
  }

  /** ファイナルフィールドかどうかを判定 */
  public boolean isFinal() {
    return java.lang.reflect.Modifier.isFinal(modifiers);
  }

  /** 揮発性フィールドかどうかを判定 */
  public boolean isVolatile() {
    return java.lang.reflect.Modifier.isVolatile(modifiers);
  }

  /** 一時的フィールドかどうかを判定 */
  public boolean isTransient() {
    return java.lang.reflect.Modifier.isTransient(modifiers);
  }
}
