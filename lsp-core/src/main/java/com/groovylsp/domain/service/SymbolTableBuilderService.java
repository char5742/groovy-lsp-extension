package com.groovylsp.domain.service;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.Scope;
import com.groovylsp.domain.model.SymbolTable;
import io.vavr.control.Either;

/**
 * シンボルテーブル構築サービスのインターフェース
 *
 * <p>ASTからシンボルテーブルとスコープ情報を構築する機能を提供します。
 */
public interface SymbolTableBuilderService {

  /**
   * ASTからシンボルテーブルを構築
   *
   * @param astInfo AST情報
   * @param uri ファイルURI
   * @param symbolTable 構築先のシンボルテーブル
   * @return 成功時はUnit、失敗時はエラーメッセージ
   */
  Either<String, Void> buildSymbolTable(AstInfo astInfo, String uri, SymbolTable symbolTable);

  /**
   * ASTからスコープ情報を構築
   *
   * @param astInfo AST情報
   * @param uri ファイルURI
   * @return ルートスコープ、またはエラー
   */
  Either<String, Scope> buildScope(AstInfo astInfo, String uri);
}
