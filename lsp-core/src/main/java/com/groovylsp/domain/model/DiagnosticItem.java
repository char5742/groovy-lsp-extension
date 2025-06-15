package com.groovylsp.domain.model;

import java.util.Objects;

/** 診断結果の個別アイテムを表すドメインモデル。 */
public record DiagnosticItem(
    DocumentPosition startPosition,
    DocumentPosition endPosition,
    DiagnosticSeverity severity,
    String message,
    String source) {

  public DiagnosticItem {
    Objects.requireNonNull(startPosition, "startPosition must not be null");
    Objects.requireNonNull(endPosition, "endPosition must not be null");
    Objects.requireNonNull(severity, "severity must not be null");
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(source, "source must not be null");
  }

  /** 診断の重要度レベル。 */
  public enum DiagnosticSeverity {
    ERROR(1),
    WARNING(2),
    INFORMATION(3),
    HINT(4);

    private final int value;

    DiagnosticSeverity(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  /** ドキュメント内の位置を表す。 */
  public record DocumentPosition(int line, int character) {
    public DocumentPosition {
      if (line < 0) {
        throw new IllegalArgumentException("line must be non-negative");
      }
      if (character < 0) {
        throw new IllegalArgumentException("character must be non-negative");
      }
    }
  }
}
