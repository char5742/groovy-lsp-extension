package com.groovylsp.application.usecase;

import com.groovylsp.domain.model.Symbol;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.SymbolExtractionService;
import io.vavr.control.Either;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ドキュメントシンボルの取得に関するユースケース
 *
 * <p>LSPのtextDocument/documentSymbolリクエストを処理し、 ドキュメント内のシンボル情報を提供します。
 */
@Singleton
public class DocumentSymbolUseCase {

  private static final Logger logger = LoggerFactory.getLogger(DocumentSymbolUseCase.class);

  private final SymbolExtractionService symbolExtractionService;
  private final TextDocumentRepository repository;

  @Inject
  public DocumentSymbolUseCase(
      SymbolExtractionService symbolExtractionService, TextDocumentRepository repository) {
    this.symbolExtractionService = symbolExtractionService;
    this.repository = repository;
  }

  /**
   * ドキュメントシンボルを取得
   *
   * @param params DocumentSymbolParams
   * @return ドキュメントシンボルのリスト、またはエラー
   */
  public Either<String, List<DocumentSymbol>> getDocumentSymbols(DocumentSymbolParams params) {
    String uri = params.getTextDocument().getUri();
    logger.debug("ドキュメントシンボルを取得: {}", uri);

    return repository
        .findByUri(URI.create(uri))
        .toEither(() -> "ドキュメントが見つかりません: " + uri)
        .flatMap(
            document -> {
              String content = document.content();
              return symbolExtractionService
                  .extractSymbols(uri, content)
                  .map(symbols -> symbols.stream().map(this::toDocumentSymbol).toList());
            });
  }

  /**
   * ドメインモデルのSymbolをLSPのDocumentSymbolに変換
   *
   * @param symbol ドメインモデルのSymbol
   * @return LSPのDocumentSymbol
   */
  private DocumentSymbol toDocumentSymbol(Symbol symbol) {
    var documentSymbol = new DocumentSymbol();
    documentSymbol.setName(symbol.name());
    documentSymbol.setKind(symbol.kind());
    documentSymbol.setRange(symbol.range());
    documentSymbol.setSelectionRange(symbol.selectionRange());
    documentSymbol.setDetail(symbol.detail());
    documentSymbol.setChildren(symbol.children().stream().map(this::toDocumentSymbol).toList());
    return documentSymbol;
  }
}
