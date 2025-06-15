package com.groovylsp.domain.model;

import java.util.Set;

/**
 * Groovy LSPの設定を表すドメインモデル。
 *
 * @param enabledFileExtensions 診断を有効にするファイル拡張子のセット
 * @param excludePatterns 診断から除外するファイルパターンのセット
 */
public record GroovyLspConfig(Set<String> enabledFileExtensions, Set<String> excludePatterns) {

  /** デフォルトの設定。 */
  public static final GroovyLspConfig DEFAULT =
      new GroovyLspConfig(Set.of(".groovy", ".gradle", ".gradle.kts"), Set.of());

  /**
   * 指定されたファイルパスが除外パターンに一致するかを判定する。
   *
   * @param filePath ファイルパス
   * @return 除外パターンに一致する場合はtrue
   */
  public boolean isExcluded(String filePath) {
    return excludePatterns.stream().anyMatch(filePath::contains);
  }
}
