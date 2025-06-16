package com.groovylsp.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.LineCountResult;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FastTest
class DiagnosticUseCaseTest {

  private LineCountService lineCountService;
  private BracketValidationService bracketValidationService;
  private AstAnalysisService astAnalysisService;

  private DiagnosticUseCase diagnosticUseCase;

  @BeforeEach
  void setUp() {
    lineCountService = mock(LineCountService.class);
    bracketValidationService = mock(BracketValidationService.class);
    astAnalysisService = mock(AstAnalysisService.class);

    // デフォルトで括弧チェックは空のリストを返すように設定
    when(bracketValidationService.validate(any()))
        .thenReturn(Either.right(io.vavr.collection.List.empty()));

    // デフォルトでASTチェックはエラーなしを返すように設定
    when(astAnalysisService.analyze(any(), any()))
        .thenReturn(Either.right(new AstInfo("", List.of(), List.of(), "", List.of())));

    diagnosticUseCase =
        new DiagnosticUseCase(lineCountService, bracketValidationService, astAnalysisService);
  }

  @Test
  void 正常に診断結果を返す() throws Exception {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content = "println 'Hello'";
    var document = new TextDocument(uri, "groovy", 1, content);
    var lineCountResult = new LineCountResult(1, 0, 0, 1);

    when(lineCountService.countLines(content)).thenReturn(Either.right(lineCountResult));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isRight()).isTrue();
    var diagnosticResult = result.get();
    assertThat(diagnosticResult.documentUri()).isEqualTo(uri);
    assertThat(diagnosticResult.hasDiagnostics()).isTrue();
    assertThat(diagnosticResult.diagnostics()).hasSize(1);

    var diagnostic = diagnosticResult.diagnostics().get(0);
    assertThat(diagnostic.severity()).isEqualTo(DiagnosticItem.DiagnosticSeverity.INFORMATION);
    assertThat(diagnostic.message()).isEqualTo("総行数: 1行 (コード: 1行, 空行: 0行, コメント: 0行)");
    assertThat(diagnostic.source()).isEqualTo("groovy-lsp-line-count");
  }

  @Test
  void 複数行のファイルを診断できる() throws Exception {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content = """
        // コメント
        println 'Hello'

        println 'World'
        """;
    var document = new TextDocument(uri, "groovy", 1, content);
    var lineCountResult = new LineCountResult(4, 1, 1, 2);

    when(lineCountService.countLines(content)).thenReturn(Either.right(lineCountResult));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isRight()).isTrue();
    var diagnosticResult = result.get();
    var diagnostic = diagnosticResult.diagnostics().get(0);
    assertThat(diagnostic.message()).isEqualTo("総行数: 4行 (コード: 2行, 空行: 1行, コメント: 1行)");
  }

  @Test
  void 空のファイルを診断できる() throws Exception {
    // Given
    var uri = URI.create("file:///empty.groovy");
    var content = "";
    var document = new TextDocument(uri, "groovy", 1, content);
    var lineCountResult = LineCountResult.empty();

    when(lineCountService.countLines(content)).thenReturn(Either.right(lineCountResult));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isRight()).isTrue();
    var diagnosticResult = result.get();
    var diagnostic = diagnosticResult.diagnostics().get(0);
    assertThat(diagnostic.message()).isEqualTo("総行数: 0行 (コード: 0行, 空行: 0行, コメント: 0行)");
  }

  @Test
  void 行カウントエラー時はエラーを返す() throws Exception {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content = "invalid content";
    var document = new TextDocument(uri, "groovy", 1, content);
    var errorMessage = "解析エラー";

    when(lineCountService.countLines(content)).thenReturn(Either.left(errorMessage));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isEqualTo("行カウントに失敗しました: " + errorMessage);
  }

  @Test
  void 診断位置は常にドキュメントの先頭を指す() throws Exception {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content = "println 'Hello'";
    var document = new TextDocument(uri, "groovy", 1, content);
    var lineCountResult = new LineCountResult(1, 0, 0, 1);

    when(lineCountService.countLines(content)).thenReturn(Either.right(lineCountResult));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isRight()).isTrue();
    var diagnostic = result.get().diagnostics().get(0);
    assertThat(diagnostic.startPosition().line()).isEqualTo(0);
    assertThat(diagnostic.startPosition().character()).isEqualTo(0);
    assertThat(diagnostic.endPosition().line()).isEqualTo(0);
    assertThat(diagnostic.endPosition().character()).isEqualTo(0);
  }

  @Test
  void AST解析による構文エラーを診断に含める() throws Exception {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content = "class Test { void method() {";
    var document = new TextDocument(uri, "groovy", 1, content);
    var lineCountResult = new LineCountResult(1, 0, 0, 1);

    var syntaxError =
        new DiagnosticItem(
            new DiagnosticItem.DocumentPosition(0, 28),
            new DiagnosticItem.DocumentPosition(0, 28),
            DiagnosticItem.DiagnosticSeverity.ERROR,
            "unexpected end of file",
            "groovy-syntax");

    var astInfo = new AstInfo(uri.toString(), List.of(), List.of(syntaxError), "", List.of());

    when(lineCountService.countLines(content)).thenReturn(Either.right(lineCountResult));
    when(astAnalysisService.analyze(uri.toString(), content)).thenReturn(Either.right(astInfo));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isRight()).isTrue();
    var diagnosticResult = result.get();
    assertThat(diagnosticResult.diagnostics())
        .hasSize(2) // 行カウント + 構文エラー
        .anySatisfy(
            diag -> {
              assertThat(diag.severity()).isEqualTo(DiagnosticItem.DiagnosticSeverity.ERROR);
              assertThat(diag.message()).isEqualTo("unexpected end of file");
              assertThat(diag.source()).isEqualTo("groovy-syntax");
            });
  }

  @Test
  void 複数の構文エラーを診断に含める() throws Exception {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content = "import \nclass Test {";
    var document = new TextDocument(uri, "groovy", 1, content);
    var lineCountResult = new LineCountResult(2, 0, 0, 2);

    var syntaxError1 =
        new DiagnosticItem(
            new DiagnosticItem.DocumentPosition(0, 6),
            new DiagnosticItem.DocumentPosition(0, 7),
            DiagnosticItem.DiagnosticSeverity.ERROR,
            "expecting EOF, found 'class'",
            "groovy-syntax");

    var syntaxError2 =
        new DiagnosticItem(
            new DiagnosticItem.DocumentPosition(1, 12),
            new DiagnosticItem.DocumentPosition(1, 12),
            DiagnosticItem.DiagnosticSeverity.ERROR,
            "unexpected end of file",
            "groovy-syntax");

    var astInfo =
        new AstInfo(uri.toString(), List.of(), List.of(syntaxError1, syntaxError2), "", List.of());

    when(lineCountService.countLines(content)).thenReturn(Either.right(lineCountResult));
    when(astAnalysisService.analyze(uri.toString(), content)).thenReturn(Either.right(astInfo));

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertThat(result.isRight()).isTrue();
    var diagnosticResult = result.get();
    var errorDiagnostics =
        diagnosticResult.diagnostics().stream()
            .filter(d -> d.severity() == DiagnosticItem.DiagnosticSeverity.ERROR)
            .toList();
    assertThat(errorDiagnostics).hasSize(2);
  }
}
