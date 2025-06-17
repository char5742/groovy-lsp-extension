package com.groovylsp.domain.service;

import com.groovylsp.domain.model.SymbolDefinition;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.eclipse.lsp4j.Position;

/**
 * 定義検索サービスのインターフェース
 *
 * <p>シンボルの定義位置を検索する機能を提供します。 textDocument/definitionリクエストの処理で使用されます。
 */
public interface DefinitionFinderService {

  /**
   * 指定位置のシンボルの定義を検索
   *
   * @param uri ファイルURI
   * @param position カーソル位置
   * @return 定義位置のリスト、またはエラー
   */
  Either<String, List<SymbolDefinition>> findDefinition(String uri, Position position);

  /**
   * シンボル名から定義を検索
   *
   * @param symbolName シンボル名
   * @param uri 検索元のファイルURI（スコープ解決のため）
   * @param position 検索元の位置（スコープ解決のため）
   * @return 定義位置のリスト、またはエラー
   */
  Either<String, List<SymbolDefinition>> findDefinitionByName(
      String symbolName, String uri, Position position);

  /**
   * 完全修飾名から定義を検索
   *
   * @param qualifiedName 完全修飾名
   * @return 定義位置、またはエラー
   */
  Either<String, SymbolDefinition> findDefinitionByQualifiedName(String qualifiedName);
}
