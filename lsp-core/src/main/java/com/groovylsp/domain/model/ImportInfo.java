package com.groovylsp.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * import文の情報を表すドメインモデル
 *
 * <p>通常のimportとimport aliasの両方に対応します。 例： - import java.time.LocalDate - import java.time.LocalDate
 * as LD
 */
public record ImportInfo(String className, @Nullable String alias) {

  /**
   * エイリアスがある場合はエイリアス、ない場合はクラス名の単純名を返す
   *
   * @return 使用する名前
   */
  public String usageName() {
    if (alias != null) {
      return alias;
    }
    // クラス名から単純名を抽出
    int lastDot = className.lastIndexOf('.');
    return lastDot >= 0 ? className.substring(lastDot + 1) : className;
  }

  /**
   * 静的importかどうかを判定
   *
   * @return 静的importの場合true
   */
  public boolean isStatic() {
    // 今のところ静的importは未対応
    return false;
  }
}
