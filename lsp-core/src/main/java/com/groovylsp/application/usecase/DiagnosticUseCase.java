package com.groovylsp.application.usecase;

import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.DiagnosticResult;
import com.groovylsp.domain.model.TextDocument;
import io.vavr.control.Either;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 診断機能のユースケース。 */
@Singleton
public class DiagnosticUseCase {

  private static final Logger logger = LoggerFactory.getLogger(DiagnosticUseCase.class);

  @Inject
  public DiagnosticUseCase() {}

  /**
   * テキストドキュメントを診断する。
   *
   * @param document 診断対象のドキュメント
   * @return 診断結果またはエラー
   */
  public Either<String, DiagnosticResult> diagnose(TextDocument document) {
    logger.info("Starting diagnosis for document: {}", document.uri());

    // Phase 1 M1.2: 固定メッセージを表示
    var diagnosticItem =
        new DiagnosticItem(
            new DiagnosticItem.DocumentPosition(0, 0),
            new DiagnosticItem.DocumentPosition(0, 0),
            DiagnosticItem.DiagnosticSeverity.INFORMATION,
            "Hello from Groovy LSP",
            "groovy-lsp");

    var result = new DiagnosticResult(document.uri(), List.of(diagnosticItem));

    logger.info("Diagnosis completed with {} items", result.diagnostics().size());
    return Either.right(result);
  }
}
