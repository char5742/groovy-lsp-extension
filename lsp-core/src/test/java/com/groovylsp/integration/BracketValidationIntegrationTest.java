package com.groovylsp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.application.usecase.DiagnosticUseCase;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.testing.IntegrationTest;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@IntegrationTest
class BracketValidationIntegrationTest {

  private DiagnosticUseCase diagnosticUseCase;

  @BeforeEach
  void setUp() {
    var lineCountService = new LineCountService();
    var bracketValidationService = new BracketValidationService();
    var parser = new GroovyAstParser();
    var astAnalysisService = new AstAnalysisService(parser);
    diagnosticUseCase =
        new DiagnosticUseCase(lineCountService, bracketValidationService, astAnalysisService);
  }

  @Test
  @DisplayName("括弧が正しく対応している場合、エラーが報告されない")
  void testValidBrackets() throws Exception {
    // given
    var content =
        """
        def method() {
            println("Hello, [World]")
        }
        """;
    var document = new TextDocument(URI.create("file:///test.groovy"), "groovy", 1, content);

    // when
    var result = diagnosticUseCase.diagnose(document);

    // then
    assertThat(result.isRight()).isTrue();
    var diagnostics = result.get().diagnostics();
    // 行カウントの診断結果のみが存在し、括弧エラーは存在しない
    assertThat(diagnostics).filteredOn(d -> "bracket-validation".equals(d.source())).isEmpty();
  }

  @Test
  @DisplayName("閉じ括弧が不足している場合、エラーが報告される")
  void testMissingClosingBracket() throws Exception {
    // given
    var content = """
        def method() {
            println("Hello, World"
        """;
    var document = new TextDocument(URI.create("file:///test.groovy"), "groovy", 1, content);

    // when
    var result = diagnosticUseCase.diagnose(document);

    // then
    assertThat(result.isRight()).isTrue();
    var diagnostics = result.get().diagnostics();
    var bracketErrors =
        diagnostics.stream().filter(d -> "bracket-validation".equals(d.source())).toList();

    // 複数のエラーが発生する可能性があるため、閉じ括弧不足のエラーを探す
    var missingClosingError =
        bracketErrors.stream().filter(e -> e.message().contains("閉じ括弧")).findFirst();

    assertThat(missingClosingError).isPresent();
    assertThat(missingClosingError.get().severity())
        .isEqualTo(DiagnosticItem.DiagnosticSeverity.ERROR);
  }

  @Test
  @DisplayName("開き括弧が不足している場合、エラーが報告される")
  void testMissingOpeningBracket() throws Exception {
    // given
    var content =
        """
        def method() {
            println "Hello, World")
        }
        """;
    var document = new TextDocument(URI.create("file:///test.groovy"), "groovy", 1, content);

    // when
    var result = diagnosticUseCase.diagnose(document);

    // then
    assertThat(result.isRight()).isTrue();
    var diagnostics = result.get().diagnostics();
    var bracketErrors =
        diagnostics.stream().filter(d -> "bracket-validation".equals(d.source())).toList();

    // 開き括弧不足のエラーを探す
    var missingOpeningError =
        bracketErrors.stream().filter(e -> e.message().contains("対応する開き括弧")).findFirst();

    assertThat(missingOpeningError).isPresent();
    assertThat(missingOpeningError.get().severity())
        .isEqualTo(DiagnosticItem.DiagnosticSeverity.ERROR);
  }

  @Test
  @DisplayName("括弧の種類が一致しない場合、エラーが報告される")
  void testMismatchedBrackets() throws Exception {
    // given
    var content = """
        def method() {
            def list = [1, 2, 3}
        }
        """;
    var document = new TextDocument(URI.create("file:///test.groovy"), "groovy", 1, content);

    // when
    var result = diagnosticUseCase.diagnose(document);

    // then
    assertThat(result.isRight()).isTrue();
    var diagnostics = result.get().diagnostics();
    var bracketErrors =
        diagnostics.stream().filter(d -> "bracket-validation".equals(d.source())).toList();

    assertThat(bracketErrors).hasSize(1);
    var error = bracketErrors.get(0);
    assertThat(error.severity()).isEqualTo(DiagnosticItem.DiagnosticSeverity.ERROR);
    assertThat(error.message()).isEqualTo("括弧の種類が一致しません: '[' に対して '}' が使用されています");
  }

  @Test
  @DisplayName("複数の括弧エラーが存在する場合、すべてが報告される")
  void testMultipleBracketErrors() throws Exception {
    // given
    var content =
        """
        def method() {
            def list = [1, 2, 3
            println("Hello"
            def map = [key: "value"}
        ]
        """;
    var document = new TextDocument(URI.create("file:///test.groovy"), "groovy", 1, content);

    // when
    var result = diagnosticUseCase.diagnose(document);

    // then
    assertThat(result.isRight()).isTrue();
    var diagnostics = result.get().diagnostics();
    var bracketErrors =
        diagnostics.stream().filter(d -> "bracket-validation".equals(d.source())).toList();

    assertThat(bracketErrors).hasSizeGreaterThan(1);
  }

  @Test
  @DisplayName("文字列内の括弧は無視される")
  void testBracketsInString() throws Exception {
    // given
    var content =
        """
        def method() {
            println("This is a ( string with ) brackets [ and ] stuff")
        }
        """;
    var document = new TextDocument(URI.create("file:///test.groovy"), "groovy", 1, content);

    // when
    var result = diagnosticUseCase.diagnose(document);

    // then
    assertThat(result.isRight()).isTrue();
    var diagnostics = result.get().diagnostics();
    // 文字列内の括弧は無視されるため、括弧エラーは存在しない
    assertThat(diagnostics).filteredOn(d -> "bracket-validation".equals(d.source())).isEmpty();
  }
}
