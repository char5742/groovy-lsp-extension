package com.groovylsp.infrastructure.ast;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.domain.model.Symbol;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import java.util.List;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** GroovySymbolExtractionServiceのテスト */
@FastTest
class GroovySymbolExtractionServiceTest {

  private GroovyAstParser parser;
  private GroovySymbolExtractionService service;

  @BeforeEach
  void setUp() {
    parser = new GroovyAstParser();
    service = new GroovySymbolExtractionService(parser);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (parser != null) {
      parser.close();
    }
  }

  @Nested
  @DisplayName("基本的なシンボル抽出")
  class BasicSymbolExtraction {

    @Test
    @DisplayName("クラスとメソッドのシンボルを抽出できる")
    void extractClassAndMethodSymbols() {
      // given
      String sourceCode =
          """
          class Calculator {
              int add(int a, int b) {
                  return a + b
              }

              int subtract(int a, int b) {
                  return a - b
              }
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/Calculator.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.name()).isEqualTo("Calculator");
      assertThat(classSymbol.kind()).isEqualTo(SymbolKind.Class);

      assertThat(classSymbol.children()).hasSize(2);
      assertThat(classSymbol.children())
          .anySatisfy(
              method -> {
                assertThat(method.name()).isEqualTo("add");
                assertThat(method.kind()).isEqualTo(SymbolKind.Method);
                assertThat(method.detail()).contains("(int a, int b): int");
              })
          .anySatisfy(
              method -> {
                assertThat(method.name()).isEqualTo("subtract");
                assertThat(method.kind()).isEqualTo(SymbolKind.Method);
                assertThat(method.detail()).contains("(int a, int b): int");
              });
    }

    @Test
    @DisplayName("フィールドとプロパティのシンボルを抽出できる")
    void extractFieldAndPropertySymbols() {
      // given
      String sourceCode =
          """
          class Person {
              private String name
              int age
              String address

              void setName(String name) {
                  this.name = name
              }
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/Person.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.name()).isEqualTo("Person");

      // フィールド、プロパティ、メソッドが含まれているか確認
      assertThat(classSymbol.children())
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("name");
                assertThat(symbol.kind()).isIn(SymbolKind.Field, SymbolKind.Property);
                assertThat(symbol.detail()).isEqualTo("String");
              })
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("age");
                assertThat(symbol.detail()).isEqualTo("int");
              })
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("address");
                assertThat(symbol.detail()).isEqualTo("String");
              });
    }
  }

  @Nested
  @DisplayName("継承とインターフェース")
  class InheritanceAndInterfaces {

    @Test
    @DisplayName("継承クラスの詳細情報を含むシンボルを抽出できる")
    void extractSymbolsWithInheritance() {
      // given
      String sourceCode =
          """
          interface Runnable {
              void run()
          }

          class Thread extends Object implements Runnable {
              void run() {
                  println "Running"
              }
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/Thread.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(2);

      // インターフェースのシンボル
      assertThat(symbols)
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("Runnable");
                assertThat(symbol.kind()).isEqualTo(SymbolKind.Interface);
              });

      // クラスのシンボル（継承情報を含む）
      assertThat(symbols)
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("Thread");
                assertThat(symbol.kind()).isEqualTo(SymbolKind.Class);
                assertThat(symbol.detail()).contains("implements Runnable");
              });
    }
  }

  @Nested
  @DisplayName("Spock固有の構文")
  class SpockSpecificSyntax {

    @Test
    @DisplayName("Spockテストクラスのシンボルを抽出できる")
    void extractSpockTestSymbols() {
      // given
      String sourceCode =
          """
          import spock.lang.Specification

          class CalculatorSpec extends Specification {
              def "addition should work correctly"() {
                  given:
                  def calculator = new Calculator()

                  when:
                  def result = calculator.add(2, 3)

                  then:
                  result == 5
              }

              def "subtraction should work correctly"() {
                  expect:
                  new Calculator().subtract(5, 3) == 2
              }
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/CalculatorSpec.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol specClass = symbols.get(0);
      assertThat(specClass.name()).isEqualTo("CalculatorSpec");
      assertThat(specClass.detail()).contains("extends Specification");

      // Spockのテストメソッドが正しく認識されているか
      assertThat(specClass.children())
          .anySatisfy(
              method -> {
                assertThat(method.name()).isEqualTo("addition should work correctly");
                assertThat(method.kind()).isEqualTo(SymbolKind.Method);
              })
          .anySatisfy(
              method -> {
                assertThat(method.name()).isEqualTo("subtraction should work correctly");
                assertThat(method.kind()).isEqualTo(SymbolKind.Method);
              });
    }
  }

  @Nested
  @DisplayName("エラーハンドリング")
  class ErrorHandling {

    @Test
    @DisplayName("パースエラーがある場合でも処理を継続する")
    void handleParseError() {
      // given
      String sourceCode =
          """
          class BrokenClass {
              void method1() {
                  println "Valid method"
              }

              // 構文エラー: 閉じ括弧がない
              void method2() {
                  println "Missing closing brace"

              void method3() {
                  println "Method after error"
              }
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/BrokenClass.groovy", sourceCode);

      // then
      // パースエラーがあってもGroovyパーサーは部分的にASTを生成する可能性がある
      // エラーの場合はLeftになるか、空のシンボルリストが返る
      if (result.isLeft()) {
        assertThat(result.getLeft()).contains("パースエラー");
      } else {
        // 部分的なASTが生成された場合
        assertThat(result.get()).isNotNull();
      }
    }
  }

  @Nested
  @DisplayName("空のソースファイル")
  class EmptySourceFile {

    @Test
    @DisplayName("空のソースファイルでもエラーにならない")
    void handleEmptySource() {
      // given
      String sourceCode = "";

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/Empty.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      // Groovyは空のファイルでもスクリプトクラスを生成するが、
      // shouldIncludeClassでスクリプトクラスを除外しているため空になる
      assertThat(symbols).isEmpty();
    }

    @Test
    @DisplayName("スクリプトのみのファイルではシンボルが抽出されない")
    void scriptOnlyFile() {
      // given
      String sourceCode =
          """
          println "Hello, World!"
          def x = 10
          def y = 20
          println x + y
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/script.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      // スクリプトクラスは除外されるため、シンボルは抽出されない
      assertThat(symbols).isEmpty();
    }
  }
}
