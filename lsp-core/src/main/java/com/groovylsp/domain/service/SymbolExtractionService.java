package com.groovylsp.domain.service;

import com.groovylsp.domain.model.Symbol;
import io.vavr.control.Either;
import java.util.List;

/**
 * ドキュメントからシンボル情報を抽出するサービス
 *
 * <p>ASTを解析して、クラス、メソッド、プロパティなどの構造化された情報を抽出します。
 */
public interface SymbolExtractionService {

  /**
   * ドキュメントからシンボル情報を抽出
   *
   * @param uri ドキュメントのURI
   * @param content ドキュメントの内容
   * @return 抽出されたシンボルのリスト、またはエラー
   */
  Either<String, List<Symbol>> extractSymbols(String uri, String content);
}
