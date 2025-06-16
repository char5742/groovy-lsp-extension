package com.groovylsp.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.application.usecase.DiagnosticUseCase;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.testing.IntegrationTest;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 診断機能の統合テスト */
@IntegrationTest
class DiagnosticIntegrationTest {

  private DiagnosticUseCase diagnosticUseCase;
  private LineCountService lineCountService;
  private BracketValidationService bracketValidationService;

  @BeforeEach
  void setUp() {
    lineCountService = new LineCountService();
    bracketValidationService = new BracketValidationService();
    diagnosticUseCase = new DiagnosticUseCase(lineCountService, bracketValidationService);
  }

  @Test
  void 行カウントと括弧チェックの両方の診断が実行される() {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content =
        """
      class Example {
        def method() {
          def list = [1, 2, 3  // 閉じ括弧がない
          return list
        }
      }
      """;
    var document = new TextDocument(uri, "groovy", 1, content);

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertTrue(result.isRight(), "診断は成功するはずです");
    var diagnosticResult = result.get();

    // 診断結果を詳細に出力
    System.out.println("診断結果:");
    diagnosticResult
        .diagnostics()
        .forEach(
            item -> {
              System.out.printf(
                  "  - 位置: (%d:%d) - (%d:%d), 重要度: %s, メッセージ: %s, ソース: %s%n",
                  item.startPosition().line(),
                  item.startPosition().character(),
                  item.endPosition().line(),
                  item.endPosition().character(),
                  item.severity(),
                  item.message(),
                  item.source());
            });

    // 診断結果の検証
    assertTrue(diagnosticResult.diagnostics().size() >= 2, "少なくとも2つの診断結果があるはずです");

    // 行カウント情報があることを確認
    var lineCountDiagnostic =
        diagnosticResult.diagnostics().stream()
            .filter(d -> d.source().equals("groovy-lsp-line-count"))
            .findFirst();
    assertTrue(lineCountDiagnostic.isPresent(), "行カウント診断があるはずです");

    // 括弧エラーがあることを確認
    var bracketErrors =
        diagnosticResult.diagnostics().stream()
            .filter(d -> d.source().equals("bracket-validation"))
            .toList();
    assertFalse(bracketErrors.isEmpty(), "括弧エラーがあるはずです");
  }

  @Test
  void 正しいコードでは括弧エラーが発生しない() {
    // Given
    var uri = URI.create("file:///test.groovy");
    var content =
        """
      class Example {
        def method() {
          def list = [1, 2, 3]
          def map = ['key': 'value']
          return list
        }
      }
      """;
    var document = new TextDocument(uri, "groovy", 1, content);

    // When
    var result = diagnosticUseCase.diagnose(document);

    // Then
    assertTrue(result.isRight(), "診断は成功するはずです");
    var diagnosticResult = result.get();

    // 括弧エラーがないことを確認
    var bracketErrors =
        diagnosticResult.diagnostics().stream()
            .filter(d -> d.source().equals("bracket-validation"))
            .toList();
    assertTrue(bracketErrors.isEmpty(), "括弧エラーはないはずです");
  }
}
