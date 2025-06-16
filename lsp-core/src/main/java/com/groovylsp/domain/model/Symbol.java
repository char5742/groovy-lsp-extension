package com.groovylsp.domain.model;

import java.util.List;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;

/**
 * ドキュメント内のシンボル情報を表すモデル
 *
 * <p>LSPのDocumentSymbolに対応し、クラス、メソッド、プロパティなどの 構造化された情報を表現します。
 */
public record Symbol(
    String name,
    SymbolKind kind,
    Range range,
    Range selectionRange,
    String detail,
    List<Symbol> children) {

  /**
   * 子要素を持たないシンボルを作成
   *
   * @param name シンボル名
   * @param kind シンボルの種類
   * @param range シンボルの全体範囲
   * @param selectionRange シンボル名の範囲
   * @param detail 詳細情報（型情報など）
   * @return 新しいSymbolインスタンス
   */
  public static Symbol create(
      String name, SymbolKind kind, Range range, Range selectionRange, String detail) {
    return new Symbol(name, kind, range, selectionRange, detail, List.of());
  }

  /**
   * 子要素を持つシンボルを作成
   *
   * @param name シンボル名
   * @param kind シンボルの種類
   * @param range シンボルの全体範囲
   * @param selectionRange シンボル名の範囲
   * @param detail 詳細情報（型情報など）
   * @param children 子シンボルのリスト
   * @return 新しいSymbolインスタンス
   */
  public static Symbol createWithChildren(
      String name,
      SymbolKind kind,
      Range range,
      Range selectionRange,
      String detail,
      List<Symbol> children) {
    return new Symbol(name, kind, range, selectionRange, detail, children);
  }
}
