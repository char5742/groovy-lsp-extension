package com.groovylsp.application.usecase;

import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.DiagnosticResult;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.infrastructure.lexer.GroovyLexer;
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
  private final BracketValidationService bracketValidationService;
  private final AstAnalysisService astAnalysisService;

  @Inject
  public DiagnosticUseCase(
      LineCountService lineCountService,
      BracketValidationService bracketValidationService,
      AstAnalysisService astAnalysisService) {
    this.lineCountService = lineCountService;
    this.bracketValidationService = bracketValidationService;
    this.astAnalysisService = astAnalysisService;
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

              // Phase 3 M3.2: AST解析による構文エラー検出
              var astResult =
                  astAnalysisService.analyze(document.uri().toString(), document.content());
              astResult
                  .peek(
                      astInfo -> {
                        // 構文エラーを診断に追加
                        diagnostics.addAll(astInfo.syntaxErrors());

                        // デバッグ情報をログ出力
                        logger.debug(
                            "AST analysis completed: {} classes found, {} syntax errors",
                            astInfo.classes().size(),
                            astInfo.syntaxErrors().size());
                        for (var classInfo : astInfo.classes()) {
                          logger.debug(
                              "Found class: {} with {} methods and {} fields",
                              classInfo.name(),
                              classInfo.methods().size(),
                              classInfo.fields().size());
                        }
                      })
                  .peekLeft(error -> logger.error("Failed to analyze AST: {}", error));

              // Phase 2 M2.3: 括弧の対応チェック
              var lexer = new GroovyLexer(document.content());
              var tokenResult = lexer.tokenize();

              tokenResult
                  .map(
                      tokens -> {
                        var bracketValidationResult = bracketValidationService.validate(tokens);
                        return bracketValidationResult
                            .getOrElse(io.vavr.collection.List.empty())
                            .toJavaList();
                      })
                  .peek(diagnostics::addAll)
                  .peekLeft(error -> logger.error("Failed to tokenize document: {}", error));

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
