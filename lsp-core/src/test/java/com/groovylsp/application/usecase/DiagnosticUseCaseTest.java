package com.groovylsp.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.LineCountResult;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FastTest
class DiagnosticUseCaseTest {

  private LineCountService lineCountService;
  private BracketValidationService bracketValidationService;

  private DiagnosticUseCase diagnosticUseCase;

  @BeforeEach
  void setUp() {
    lineCountService = mock(LineCountService.class);
    bracketValidationService = mock(BracketValidationService.class);

    // デフォルトで括弧チェックは空のリストを返すように設定
    when(bracketValidationService.validate(any()))
        .thenReturn(Either.right(io.vavr.collection.List.empty()));

    diagnosticUseCase = new DiagnosticUseCase(lineCountService, bracketValidationService);
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
}
