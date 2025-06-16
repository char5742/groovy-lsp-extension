package com.groovylsp.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovylsp.domain.model.Symbol;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.SymbolExtractionService;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.net.URI;
import java.util.List;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DocumentSymbolUseCaseのテスト */
@FastTest
class DocumentSymbolUseCaseTest {

  private SymbolExtractionService symbolExtractionService;
  private TextDocumentRepository repository;
  private DocumentSymbolUseCase useCase;

  @BeforeEach
  void setUp() {
    symbolExtractionService = mock(SymbolExtractionService.class);
    repository = mock(TextDocumentRepository.class);
    useCase = new DocumentSymbolUseCase(symbolExtractionService, repository);
  }

  @Test
  @DisplayName("ドキュメントシンボルを正常に取得できる")
  void getDocumentSymbolsSuccess() {
    // given
    String uri = "file:///test/Calculator.groovy";
    String content = "class Calculator { }";

    var params = new DocumentSymbolParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));

    var range = new Range(new Position(0, 0), new Position(0, 20));
    var selectionRange = new Range(new Position(0, 6), new Position(0, 16));
    var symbol = Symbol.create("Calculator", SymbolKind.Class, range, selectionRange, "");

    when(symbolExtractionService.extractSymbols(anyString(), anyString()))
        .thenReturn(Either.right(List.of(symbol)));

    // when
    Either<String, List<DocumentSymbol>> result = useCase.getDocumentSymbols(params);

    // then
    assertThat(result.isRight()).isTrue();
    List<DocumentSymbol> documentSymbols = result.get();
    assertThat(documentSymbols).hasSize(1);

    DocumentSymbol docSymbol = documentSymbols.get(0);
    assertThat(docSymbol.getName()).isEqualTo("Calculator");
    assertThat(docSymbol.getKind()).isEqualTo(SymbolKind.Class);
    assertThat(docSymbol.getRange()).isEqualTo(range);
    assertThat(docSymbol.getSelectionRange()).isEqualTo(selectionRange);
  }

  @Test
  @DisplayName("ドキュメントが見つからない場合はエラーを返す")
  void getDocumentSymbolsDocumentNotFound() {
    // given
    String uri = "file:///test/NotFound.groovy";

    var params = new DocumentSymbolParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));

    when(repository.findByUri(URI.create(uri))).thenReturn(Option.none());

    // when
    Either<String, List<DocumentSymbol>> result = useCase.getDocumentSymbols(params);

    // then
    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).contains("ドキュメントが見つかりません: " + uri);
  }

  @Test
  @DisplayName("シンボル抽出でエラーが発生した場合はエラーを返す")
  void getDocumentSymbolsExtractionError() {
    // given
    String uri = "file:///test/Error.groovy";
    String content = "class Error { }";

    var params = new DocumentSymbolParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));

    when(symbolExtractionService.extractSymbols(anyString(), anyString()))
        .thenReturn(Either.left("Parse error"));

    // when
    Either<String, List<DocumentSymbol>> result = useCase.getDocumentSymbols(params);

    // then
    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).contains("Parse error");
  }

  @Test
  @DisplayName("ネストされたシンボルを正しく変換できる")
  void getDocumentSymbolsWithNestedSymbols() {
    // given
    String uri = "file:///test/Nested.groovy";
    String content = "class Outer { class Inner { } }";

    var params = new DocumentSymbolParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));

    // 内部クラスのシンボル
    var innerRange = new Range(new Position(0, 14), new Position(0, 29));
    var innerSelectionRange = new Range(new Position(0, 20), new Position(0, 25));
    var innerSymbol = Symbol.create("Inner", SymbolKind.Class, innerRange, innerSelectionRange, "");

    // 外部クラスのシンボル（内部クラスを含む）
    var outerRange = new Range(new Position(0, 0), new Position(0, 31));
    var outerSelectionRange = new Range(new Position(0, 6), new Position(0, 11));
    var outerSymbol =
        Symbol.createWithChildren(
            "Outer", SymbolKind.Class, outerRange, outerSelectionRange, "", List.of(innerSymbol));

    when(symbolExtractionService.extractSymbols(anyString(), anyString()))
        .thenReturn(Either.right(List.of(outerSymbol)));

    // when
    Either<String, List<DocumentSymbol>> result = useCase.getDocumentSymbols(params);

    // then
    assertThat(result.isRight()).isTrue();
    List<DocumentSymbol> documentSymbols = result.get();
    assertThat(documentSymbols).hasSize(1);

    DocumentSymbol outer = documentSymbols.get(0);
    assertThat(outer.getName()).isEqualTo("Outer");
    assertThat(outer.getChildren()).hasSize(1);

    DocumentSymbol inner = outer.getChildren().get(0);
    assertThat(inner.getName()).isEqualTo("Inner");
    assertThat(inner.getKind()).isEqualTo(SymbolKind.Class);
  }

  @Test
  @DisplayName("空のファイルの場合は空のリストを返す")
  void getDocumentSymbolsEmptyFile() {
    // given
    String uri = "file:///test/Empty.groovy";
    String content = "";

    var params = new DocumentSymbolParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));

    when(symbolExtractionService.extractSymbols(anyString(), anyString()))
        .thenReturn(Either.right(List.of()));

    // when
    Either<String, List<DocumentSymbol>> result = useCase.getDocumentSymbols(params);

    // then
    assertThat(result.isRight()).isTrue();
    List<DocumentSymbol> documentSymbols = result.get();
    assertThat(documentSymbols).isEmpty();
  }
}
