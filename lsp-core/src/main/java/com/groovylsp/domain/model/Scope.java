package com.groovylsp.domain.model;

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.Nullable;

/**
 * スコープを表すドメインモデル
 *
 * <p>変数やメソッドの可視範囲を管理し、名前解決に使用されます。 階層的なスコープ構造をサポートし、親スコープへの参照を持ちます。
 */
public class Scope {

  /** スコープの種類 */
  public enum ScopeType {
    /** グローバルスコープ（ファイルレベル） */
    GLOBAL,
    /** クラススコープ */
    CLASS,
    /** メソッドスコープ */
    METHOD,
    /** ブロックスコープ（if, for, while等） */
    BLOCK,
    /** クロージャスコープ */
    CLOSURE
  }

  private final ScopeType type;
  private final @Nullable Scope parent;
  private final Range range;
  private final @Nullable String name;
  private Map<String, SymbolDefinition> symbols;
  private List<Scope> children;

  /**
   * スコープを作成
   *
   * @param type スコープの種類
   * @param parent 親スコープ（ルートスコープの場合はnull）
   * @param range スコープの範囲
   * @param name スコープの名前（クラスやメソッドの名前）
   */
  public Scope(ScopeType type, @Nullable Scope parent, Range range, @Nullable String name) {
    this.type = type;
    this.parent = parent;
    this.range = range;
    this.name = name;
    this.symbols = HashMap.empty();
    this.children = List.empty();

    // 親スコープに子として追加
    if (parent != null) {
      parent.addChild(this);
    }
  }

  /**
   * シンボルを追加
   *
   * @param symbol シンボル定義
   */
  public void addSymbol(SymbolDefinition symbol) {
    symbols = symbols.put(symbol.name(), symbol);
  }

  /**
   * 子スコープを追加
   *
   * @param child 子スコープ
   */
  private void addChild(Scope child) {
    children = children.append(child);
  }

  /**
   * 名前でシンボルを検索（親スコープも含めて）
   *
   * @param name シンボル名
   * @return 見つかったシンボル定義
   */
  public Option<SymbolDefinition> findSymbol(String name) {
    // まず現在のスコープで検索
    Option<SymbolDefinition> local = symbols.get(name);
    if (local.isDefined()) {
      return local;
    }

    // 見つからなければ親スコープで検索
    if (parent != null) {
      return parent.findSymbol(name);
    }

    return Option.none();
  }

  /**
   * 現在のスコープのみでシンボルを検索
   *
   * @param name シンボル名
   * @return 見つかったシンボル定義
   */
  public Option<SymbolDefinition> findLocalSymbol(String name) {
    return symbols.get(name);
  }

  /**
   * 指定位置を含むスコープを検索
   *
   * @param line 行番号（0ベース）
   * @param column 列番号（0ベース）
   * @return 見つかったスコープ
   */
  public Option<Scope> findScopeAt(int line, int column) {
    // 範囲内かチェック
    if (!isInRange(line, column)) {
      return Option.none();
    }

    // 子スコープで検索
    for (Scope child : children) {
      Option<Scope> found = child.findScopeAt(line, column);
      if (found.isDefined()) {
        return found;
      }
    }

    // 子スコープに見つからなければ自身を返す
    return Option.of(this);
  }

  /** 指定位置がこのスコープの範囲内かチェック */
  private boolean isInRange(int line, int column) {
    return line >= range.getStart().getLine()
        && line <= range.getEnd().getLine()
        && (line > range.getStart().getLine() || column >= range.getStart().getCharacter())
        && (line < range.getEnd().getLine() || column <= range.getEnd().getCharacter());
  }

  /**
   * すべてのローカルシンボルを取得
   *
   * @return シンボル定義のリスト
   */
  public List<SymbolDefinition> getLocalSymbols() {
    return symbols.values().toList();
  }

  /**
   * 利用可能なすべてのシンボルを取得（親スコープも含む）
   *
   * @return シンボル定義のリスト
   */
  public List<SymbolDefinition> getAllAvailableSymbols() {
    List<SymbolDefinition> available = getLocalSymbols();

    if (parent != null) {
      available = available.appendAll(parent.getAllAvailableSymbols());
    }

    return available;
  }

  // ゲッターメソッド
  public ScopeType getType() {
    return type;
  }

  public @Nullable Scope getParent() {
    return parent;
  }

  public Range getRange() {
    return range;
  }

  public @Nullable String getName() {
    return name;
  }

  public List<Scope> getChildren() {
    return children;
  }
}
