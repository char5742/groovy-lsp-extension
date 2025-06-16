package com.groovylsp.infrastructure.parser;

import groovy.lang.GroovyClassLoader;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Groovy公式パーサーを使用したAST解析器
 *
 * <p>このクラスはGroovyソースコードを解析し、抽象構文木（AST）を生成します。 LSPの各機能（診断、補完、定義ジャンプなど）で使用されます。
 */
@NullMarked
public class GroovyAstParser {

  private final CompilerConfiguration config;
  private final GroovyClassLoader classLoader;

  public GroovyAstParser() {
    this.config = new CompilerConfiguration();
    // Parrot parser（Groovy 3.0+のデフォルト）を明示的に使用
    config.getOptimizationOptions().put("groovydoc", false);
    config.setTolerance(Integer.MAX_VALUE); // エラーがあっても解析を続行

    // GroovyClassLoaderの初期化時に親クラスローダーを明示的に指定
    ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
    if (parentClassLoader == null) {
      parentClassLoader = GroovyAstParser.class.getClassLoader();
    }
    this.classLoader = new GroovyClassLoader(parentClassLoader, config);
  }

  /**
   * Groovyソースコードを解析してASTを生成
   *
   * @param fileName ファイル名
   * @param sourceCode ソースコード
   * @return 解析結果（成功時: ParseResult、失敗時: ParseError）
   */
  public Either<ParseError, ParseResult> parse(String fileName, String sourceCode) {
    List<ParseDiagnostic> diagnostics = new ArrayList<>();

    try {
      var compilationUnit = new CompilationUnit(config, null, classLoader);
      var errorCollector = new ErrorCollector(config);
      var sourceUnit = new SourceUnit(fileName, sourceCode, config, classLoader, errorCollector);
      compilationUnit.addSource(sourceUnit);

      // フェーズ2（CONVERSION）まで実行してASTを生成
      try {
        compilationUnit.compile(Phases.CONVERSION);
      } catch (org.codehaus.groovy.control.MultipleCompilationErrorsException e) {
        // コンパイルエラーは想定内なので、エラー情報を収集する
      }

      // エラーメッセージを収集
      if (errorCollector.hasErrors()) {
        for (int i = 0; i < errorCollector.getErrorCount(); i++) {
          Message error = errorCollector.getError(i);
          if (error instanceof SyntaxErrorMessage syntaxError) {
            SyntaxException cause = syntaxError.getCause();
            diagnostics.add(
                new ParseDiagnostic(
                    cause.getMessage(),
                    cause.getLine(),
                    cause.getStartColumn(),
                    cause.getEndColumn(),
                    ParseDiagnostic.Severity.ERROR));
          }
        }
      }

      // 警告メッセージを収集
      if (errorCollector.hasWarnings()) {
        for (int i = 0; i < errorCollector.getWarningCount(); i++) {
          Message warning = errorCollector.getWarning(i);
          if (warning instanceof SyntaxErrorMessage syntaxWarning) {
            SyntaxException cause = syntaxWarning.getCause();
            diagnostics.add(
                new ParseDiagnostic(
                    cause.getMessage(),
                    cause.getLine(),
                    cause.getStartColumn(),
                    cause.getEndColumn(),
                    ParseDiagnostic.Severity.WARNING));
          }
        }
      }

      ModuleNode moduleNode = sourceUnit.getAST();
      return Either.right(new ParseResult(moduleNode, diagnostics));

    } catch (Exception e) {
      return Either.left(new ParseError("パース中に予期しないエラーが発生しました: " + e.getMessage(), e));
    }
  }

  /** 解析結果 */
  public record ParseResult(ModuleNode moduleNode, List<ParseDiagnostic> diagnostics) {
    /** すべてのクラスノードを取得 */
    public List<ClassNode> getClasses() {
      return new ArrayList<>(moduleNode.getClasses());
    }
  }

  /** 解析診断情報 */
  public record ParseDiagnostic(
      String message, int line, int startColumn, int endColumn, Severity severity) {
    public enum Severity {
      ERROR,
      WARNING,
      INFO
    }
  }

  /** 解析エラー */
  public record ParseError(String message, @Nullable Throwable cause) {}
}
