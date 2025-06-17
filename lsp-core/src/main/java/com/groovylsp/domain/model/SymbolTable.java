package com.groovylsp.domain.model;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashMultimap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.util.concurrent.ConcurrentHashMap;

/**
 * シンボルと定義位置のマッピングを管理するシンボルテーブル
 *
 * <p>ファイルごとにシンボルの定義情報を保持し、高速な検索を可能にします。 名前による検索、完全修飾名による検索、ファイル内のシンボル一覧取得などの機能を提供します。
 */
public class SymbolTable {

  /** ファイルURIごとのシンボル定義 */
  private Multimap<String, SymbolDefinition> symbolsByFile;

  /** 名前によるシンボル定義のマッピング */
  private Multimap<String, SymbolDefinition> symbolsByName;

  /** 完全修飾名によるシンボル定義のマッピング */
  private Map<String, SymbolDefinition> symbolsByQualifiedName;

  /** 頻繁にアクセスされるシンボル名のキャッシュ */
  private final ConcurrentHashMap<String, List<SymbolDefinition>> nameCache;

  public SymbolTable() {
    this.symbolsByFile = HashMultimap.withSeq().empty();
    this.symbolsByName = HashMultimap.withSeq().empty();
    this.symbolsByQualifiedName = HashMap.empty();
    this.nameCache = new ConcurrentHashMap<>();
  }

  /**
   * シンボル定義を追加
   *
   * @param definition シンボル定義
   */
  public void addSymbol(SymbolDefinition definition) {
    // ファイルごとのマッピングに追加
    symbolsByFile = symbolsByFile.put(definition.uri(), definition);

    // 名前によるマッピングに追加
    symbolsByName = symbolsByName.put(definition.name(), definition);

    // キャッシュを無効化
    nameCache.remove(definition.name());

    // 完全修飾名によるマッピングに追加
    symbolsByQualifiedName = symbolsByQualifiedName.put(definition.qualifiedName(), definition);
  }

  /**
   * 複数のシンボル定義を一括追加
   *
   * @param definitions シンボル定義のリスト
   */
  public void addSymbols(List<SymbolDefinition> definitions) {
    definitions.forEach(this::addSymbol);
  }

  /**
   * ファイルのシンボル情報をクリア
   *
   * @param uri ファイルURI
   */
  public void clearFile(String uri) {
    // 該当ファイルのシンボルを取得
    var fileSymbolsOpt = symbolsByFile.get(uri);

    fileSymbolsOpt.forEach(
        fileSymbols -> {
          // 各シンボルを名前と完全修飾名のマッピングから削除
          fileSymbols.forEach(
              symbol -> {
                // 名前によるマッピングから削除
                symbolsByName = symbolsByName.remove(symbol.name(), symbol);

                // キャッシュを無効化
                nameCache.remove(symbol.name());

                // 完全修飾名によるマッピングから削除
                symbolsByQualifiedName = symbolsByQualifiedName.remove(symbol.qualifiedName());
              });
        });

    // ファイルごとのマッピングから削除
    symbolsByFile = symbolsByFile.remove(uri);
  }

  /**
   * 名前でシンボルを検索
   *
   * @param name シンボル名
   * @return 見つかったシンボル定義のリスト
   */
  public List<SymbolDefinition> findByName(String name) {
    // キャッシュから取得を試みる
    List<SymbolDefinition> cached = nameCache.get(name);
    if (cached != null) {
      return cached;
    }

    // キャッシュにない場合は通常の検索
    List<SymbolDefinition> result =
        symbolsByName.get(name).map(List::ofAll).getOrElse(List.empty());

    // 結果をキャッシュに保存（頻繁にアクセスされる場合のみ）
    if (!result.isEmpty()) {
      nameCache.put(name, result);
    }

    return result;
  }

  /**
   * 完全修飾名でシンボルを検索
   *
   * @param qualifiedName 完全修飾名
   * @return 見つかったシンボル定義（Optional）
   */
  public Option<SymbolDefinition> findByQualifiedName(String qualifiedName) {
    return symbolsByQualifiedName.get(qualifiedName);
  }

  /**
   * ファイル内のシンボルを取得
   *
   * @param uri ファイルURI
   * @return シンボル定義のリスト
   */
  public List<SymbolDefinition> getSymbolsInFile(String uri) {
    return symbolsByFile.get(uri).map(List::ofAll).getOrElse(List.empty());
  }

  /**
   * 特定の種類のシンボルを検索
   *
   * @param uri ファイルURI
   * @param type 定義の種類
   * @return シンボル定義のリスト
   */
  public List<SymbolDefinition> findByType(String uri, SymbolDefinition.DefinitionType type) {
    return getSymbolsInFile(uri).filter(symbol -> symbol.definitionType() == type);
  }

  /**
   * クラスに属するシンボルを検索
   *
   * @param qualifiedClassName 完全修飾クラス名
   * @return シンボル定義のリスト
   */
  public List<SymbolDefinition> findByContainingClass(String qualifiedClassName) {
    return symbolsByName
        .values()
        .filter(symbol -> qualifiedClassName.equals(symbol.containingClass()))
        .toList();
  }

  /**
   * すべてのファイルURIを取得
   *
   * @return ファイルURIのセット
   */
  public Set<String> getAllFileUris() {
    return symbolsByFile.keySet();
  }

  /**
   * シンボルテーブルが空かどうか
   *
   * @return 空の場合true
   */
  public boolean isEmpty() {
    return symbolsByFile.isEmpty();
  }

  /** シンボルテーブルをクリア */
  public void clear() {
    symbolsByFile = HashMultimap.withSeq().empty();
    symbolsByName = HashMultimap.withSeq().empty();
    symbolsByQualifiedName = HashMap.empty();
    nameCache.clear();
  }
}
