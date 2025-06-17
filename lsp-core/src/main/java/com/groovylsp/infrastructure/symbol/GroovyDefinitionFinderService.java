package com.groovylsp.infrastructure.symbol;

import com.groovylsp.domain.constant.ErrorMessages;
import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolDefinition;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.service.DefinitionFinderService;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import javax.inject.Inject;
import org.eclipse.lsp4j.Position;

/** Groovyソースコードの定義検索サービスの実装 */
public class GroovyDefinitionFinderService implements DefinitionFinderService {

  private final SymbolTable symbolTable;
  private final ScopeManager scopeManager;
  private final DocumentContentService documentContentService;

  @Inject
  public GroovyDefinitionFinderService(
      SymbolTable symbolTable,
      ScopeManager scopeManager,
      DocumentContentService documentContentService) {
    this.symbolTable = symbolTable;
    this.scopeManager = scopeManager;
    this.documentContentService = documentContentService;
  }

  @Override
  public Either<String, List<SymbolDefinition>> findDefinition(String uri, Position position) {
    // カーソル位置の単語を取得
    Either<String, String> wordResult = getWordAtPosition(uri, position);
    if (wordResult.isLeft()) {
      return Either.left(wordResult.getLeft());
    }

    String word = wordResult.get();
    if (word.isEmpty()) {
      return Either.right(List.empty());
    }

    // シンボル名から定義を検索
    return findDefinitionByName(word, uri, position);
  }

  @Override
  public Either<String, List<SymbolDefinition>> findDefinitionByName(
      String symbolName, String uri, Position position) {

    // まず、現在のスコープで検索
    Option<SymbolDefinition> scopedSymbol = scopeManager.findSymbolAt(uri, position, symbolName);
    if (scopedSymbol.isDefined()) {
      return Either.right(List.of(scopedSymbol.get()));
    }

    // スコープで見つからなければ、シンボルテーブルから検索
    List<SymbolDefinition> definitions = symbolTable.findByName(symbolName);

    // 同じファイル内の定義を優先
    List<SymbolDefinition> sameFileDefinitions = definitions.filter(def -> def.uri().equals(uri));
    if (!sameFileDefinitions.isEmpty()) {
      return Either.right(sameFileDefinitions);
    }

    // 他のファイルの定義を返す
    return Either.right(definitions);
  }

  @Override
  public Either<String, SymbolDefinition> findDefinitionByQualifiedName(String qualifiedName) {
    return symbolTable
        .findByQualifiedName(qualifiedName)
        .toEither(String.format(ErrorMessages.DEFINITION_NOT_FOUND, qualifiedName));
  }

  /** 指定位置の単語を取得 */
  private Either<String, String> getWordAtPosition(String uri, Position position) {
    return documentContentService
        .getContent(uri)
        .toEither(String.format(ErrorMessages.DOCUMENT_NOT_FOUND, uri))
        .map(
            content -> {
              String[] lines = content.split("\n");
              if (position.getLine() >= lines.length) {
                return "";
              }

              String line = lines[position.getLine()];
              int pos = position.getCharacter();

              // 単語の開始位置を見つける
              int start = pos;
              while (start > 0 && isWordChar(line.charAt(start - 1))) {
                start--;
              }

              // 単語の終了位置を見つける
              int end = pos;
              while (end < line.length() && isWordChar(line.charAt(end))) {
                end++;
              }

              return line.substring(start, end);
            });
  }

  /** 文字が単語の一部かどうかを判定 */
  private boolean isWordChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }
}
