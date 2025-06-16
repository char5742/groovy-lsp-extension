package com.groovylsp.domain.constant;

/**
 * エラーメッセージの定数定義
 *
 * <p>国際化対応を考慮して、エラーメッセージを一元管理します。
 */
public final class ErrorMessages {

  private ErrorMessages() {
    // ユーティリティクラスのため、インスタンス化を防ぐ
  }

  // Document関連
  public static final String DOCUMENT_NOT_FOUND = "ドキュメントが見つかりません: %s";
  public static final String DOCUMENT_UPDATE_FAILED = "ドキュメントの更新に失敗しました: %s";

  // Symbol関連
  public static final String SYMBOL_NOT_FOUND = "シンボルが見つかりません: %s";
  public static final String DEFINITION_NOT_FOUND = "定義が見つかりません: %s";

  // Parser関連
  public static final String PARSE_ERROR = "構文解析エラー: %s";
  public static final String INVALID_SYNTAX = "無効な構文です: %s";

  // File関連
  public static final String FILE_NOT_FOUND = "ファイルが見つかりません: %s";
  public static final String FILE_READ_ERROR = "ファイルの読み込みに失敗しました: %s";

  // Configuration関連
  public static final String INVALID_CONFIGURATION = "無効な設定です: %s";
  public static final String CONFIGURATION_LOAD_ERROR = "設定の読み込みに失敗しました: %s";

  // General
  public static final String INTERNAL_ERROR = "内部エラーが発生しました: %s";
  public static final String UNSUPPORTED_OPERATION = "サポートされていない操作です: %s";
  public static final String INVALID_ARGUMENT = "無効な引数です: %s";
}
