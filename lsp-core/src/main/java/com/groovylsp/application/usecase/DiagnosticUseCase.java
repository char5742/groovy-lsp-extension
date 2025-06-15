package com.groovylsp.application.usecase;

import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.DiagnosticResult;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.service.LineCountService;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 診断機能のユースケース。 */
@Singleton
public class DiagnosticUseCase {

  private static final Logger logger = LoggerFactory.getLogger(DiagnosticUseCase.class);
  private final LineCountService lineCountService;

  @Inject
  public DiagnosticUseCase(LineCountService lineCountService) {
    this.lineCountService = lineCountService;
  }

  /**
   * テキストドキュメントを診断する。
   *
   * @param document 診断対象のドキュメント
   * @return 診断結果またはエラー
   */
  public Either<String, DiagnosticResult> diagnose(TextDocument document) {
    logger.info("Starting diagnosis for document: {}", document.uri());

    List<DiagnosticItem> diagnostics = new ArrayList<>();

    // Phase 2 M2.1: 行カウント機能
    var lineCountResult = lineCountService.countLines(document.content());

    return lineCountResult
        .map(
            result -> {
              // 行カウント結果を診断メッセージとして追加
              var lineCountItem =
                  new DiagnosticItem(
                      new DiagnosticItem.DocumentPosition(0, 0),
                      new DiagnosticItem.DocumentPosition(0, 0),
                      DiagnosticItem.DiagnosticSeverity.INFORMATION,
                      result.toFormattedString(),
                      "groovy-lsp-line-count");

              diagnostics.add(lineCountItem);

              var diagnosticResult = new DiagnosticResult(document.uri(), diagnostics);
              logger.info(
                  "Diagnosis completed with {} items", diagnosticResult.diagnostics().size());
              return diagnosticResult;
            })
        .mapLeft(
            error -> {
              logger.error("Failed to count lines: {}", error);
              return "行カウントに失敗しました: " + error;
            });
  }
}
