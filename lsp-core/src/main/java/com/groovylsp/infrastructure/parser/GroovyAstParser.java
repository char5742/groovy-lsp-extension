package com.groovylsp.infrastructure.parser;

import groovy.lang.GroovyClassLoader;
import io.vavr.control.Either;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Groovy公式パーサーを使用したAST解析器
 *
 * <p>このクラスはGroovyソースコードを解析し、抽象構文木（AST）を生成します。 LSPの各機能（診断、補完、定義ジャンプなど）で使用されます。
 *
 * <p>スレッドセーフ: このクラスのインスタンスは複数のスレッドから安全に使用できます。 内部的に各パース操作は独立したCompilationUnitで実行されます。
 */
@NullMarked
public class GroovyAstParser implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(GroovyAstParser.class);

  private final ParserConfiguration configuration;
  private final Map<Thread, GroovyClassLoader> threadLocalClassLoaders = new ConcurrentHashMap<>();

  /** デフォルト設定でパーサーを作成 */
  public GroovyAstParser() {
    this(ParserConfiguration.defaultConfig());
  }

  /** カスタム設定でパーサーを作成 */
  public GroovyAstParser(ParserConfiguration configuration) {
    this.configuration = configuration;
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

    // スレッドごとに独立したClassLoaderを使用
    var classLoader = getOrCreateClassLoader();

    try {
      var compilationUnit =
          new CompilationUnit(configuration.toCompilerConfiguration(), null, classLoader);
      var errorCollector = new ErrorCollector(configuration.toCompilerConfiguration());
      var sourceUnit =
          new SourceUnit(
              fileName,
              sourceCode,
              configuration.toCompilerConfiguration(),
              classLoader,
              errorCollector);
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
                    Position.fromLineColumn(cause.getLine(), cause.getStartColumn()),
                    Position.fromLineColumn(cause.getLine(), cause.getEndColumn()),
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
                    Position.fromLineColumn(cause.getLine(), cause.getStartColumn()),
                    Position.fromLineColumn(cause.getLine(), cause.getEndColumn()),
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

  /** スレッドローカルなClassLoaderを取得または作成 */
  private GroovyClassLoader getOrCreateClassLoader() {
    return threadLocalClassLoaders.computeIfAbsent(
        Thread.currentThread(),
        thread -> {
          ClassLoader parentClassLoader = thread.getContextClassLoader();
          if (parentClassLoader == null) {
            parentClassLoader = GroovyAstParser.class.getClassLoader();
          }
          return new GroovyClassLoader(parentClassLoader, configuration.toCompilerConfiguration());
        });
  }

  /** 解析結果 */
  public record ParseResult(@Nullable ModuleNode moduleNode, List<ParseDiagnostic> diagnostics) {
    /** すべてのクラスノードを取得 */
    public List<ClassNode> getClasses() {
      if (moduleNode == null) {
        return List.of();
      }
      var classes = moduleNode.getClasses();
      return classes != null ? new ArrayList<>(classes) : List.of();
    }
  }

  /** 解析診断情報 */
  public record ParseDiagnostic(String message, Position start, Position end, Severity severity) {
    public enum Severity {
      ERROR,
      WARNING,
      INFO
    }
  }

  /** 位置情報（LSP準拠） */
  public record Position(int line, int character, int offset) {
    /** 行と列から位置情報を作成（オフセットは不明） */
    public static Position fromLineColumn(int line, int column) {
      return new Position(line, column, -1);
    }
  }

  /** 解析エラー */
  public record ParseError(String message, @Nullable Throwable cause) {}

  /** パーサー設定 */
  public record ParserConfiguration(
      int tolerance, boolean optimizeGroovydoc, Map<String, Object> additionalOptions) {

    /** デフォルト設定を取得 */
    public static ParserConfiguration defaultConfig() {
      return new ParserConfiguration(
          Integer.MAX_VALUE, // エラーがあっても解析を続行
          false, // groovydocの最適化を無効化
          Map.of() // 追加オプションなし
          );
    }

    /** CompilerConfigurationに変換 */
    public CompilerConfiguration toCompilerConfiguration() {
      var config = new CompilerConfiguration();
      config.setTolerance(tolerance);
      config.getOptimizationOptions().put("groovydoc", optimizeGroovydoc);
      additionalOptions.forEach(
          (key, value) -> {
            if (value instanceof Boolean) {
              config.getOptimizationOptions().put(key, (Boolean) value);
            } else {
              // 他の型のオプションは文字列として扱う
              config.getOptimizationOptions().put(key, Boolean.valueOf(value.toString()));
            }
          });
      return config;
    }
  }

  @Override
  public void close() {
    // すべてのスレッドローカルなClassLoaderをクローズ
    threadLocalClassLoaders
        .values()
        .forEach(
            classLoader -> {
              try {
                classLoader.close();
              } catch (IOException e) {
                logger.warn("GroovyClassLoaderのクローズ中にエラーが発生しました", e);
              }
            });
    threadLocalClassLoaders.clear();
  }
}
