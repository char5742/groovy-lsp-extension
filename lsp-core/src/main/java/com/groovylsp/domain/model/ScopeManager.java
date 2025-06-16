package com.groovylsp.domain.model;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.eclipse.lsp4j.Position;

/**
 * ファイルごとのスコープを管理するマネージャー
 *
 * <p>各ファイルのルートスコープを保持し、位置情報から適切なスコープを検索する機能を提供します。
 */
public class ScopeManager {

  /** ファイルURIごとのルートスコープ */
  private Map<String, Scope> rootScopes;

  public ScopeManager() {
    this.rootScopes = HashMap.empty();
  }

  /**
   * ファイルのルートスコープを設定
   *
   * @param uri ファイルURI
   * @param rootScope ルートスコープ
   */
  public void setRootScope(String uri, Scope rootScope) {
    rootScopes = rootScopes.put(uri, rootScope);
  }

  /**
   * ファイルのルートスコープを取得
   *
   * @param uri ファイルURI
   * @return ルートスコープ
   */
  public Option<Scope> getRootScope(String uri) {
    return rootScopes.get(uri);
  }

  /**
   * 指定位置のスコープを取得
   *
   * @param uri ファイルURI
   * @param position 位置情報
   * @return 見つかったスコープ
   */
  public Option<Scope> getScopeAt(String uri, Position position) {
    return getRootScope(uri)
        .flatMap(root -> root.findScopeAt(position.getLine(), position.getCharacter()));
  }

  /**
   * 指定位置で利用可能なシンボルを検索
   *
   * @param uri ファイルURI
   * @param position 位置情報
   * @param symbolName シンボル名
   * @return 見つかったシンボル定義
   */
  public Option<SymbolDefinition> findSymbolAt(String uri, Position position, String symbolName) {
    return getScopeAt(uri, position).flatMap(scope -> scope.findSymbol(symbolName));
  }

  /**
   * 指定位置で利用可能なすべてのシンボルを取得
   *
   * @param uri ファイルURI
   * @param position 位置情報
   * @return シンボル定義のリスト
   */
  public io.vavr.collection.List<SymbolDefinition> getAvailableSymbolsAt(
      String uri, Position position) {
    return getScopeAt(uri, position)
        .map(scope -> scope.getAllAvailableSymbols())
        .getOrElse(io.vavr.collection.List.empty());
  }

  /**
   * ファイルのスコープ情報をクリア
   *
   * @param uri ファイルURI
   */
  public void clearFile(String uri) {
    rootScopes = rootScopes.remove(uri);
  }

  /** すべてのスコープ情報をクリア */
  public void clear() {
    rootScopes = HashMap.empty();
  }

  /**
   * 管理しているファイル数を取得
   *
   * @return ファイル数
   */
  public int getFileCount() {
    return rootScopes.size();
  }
}
