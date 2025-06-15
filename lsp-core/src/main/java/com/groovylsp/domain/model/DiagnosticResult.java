package com.groovylsp.domain.model;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/** ドキュメントの診断結果を表すドメインモデル。 */
public record DiagnosticResult(URI documentUri, List<DiagnosticItem> diagnostics) {

  public DiagnosticResult {
    Objects.requireNonNull(documentUri, "documentUri must not be null");
    Objects.requireNonNull(diagnostics, "diagnostics must not be null");
    // イミュータブルにする
    diagnostics = List.copyOf(diagnostics);
  }

  /** 空の診断結果を作成する。 */
  public static DiagnosticResult empty(URI documentUri) {
    return new DiagnosticResult(documentUri, List.of());
  }

  /** 診断アイテムが存在するかどうかを判定する。 */
  public boolean hasDiagnostics() {
    return !diagnostics.isEmpty();
  }
}
