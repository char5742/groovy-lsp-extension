package com.groovylsp.infrastructure.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import java.util.List;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** GroovyAstParserのテスト */
@FastTest
class GroovyAstParserTest {

  private GroovyAstParser parser;

  @BeforeEach
  void setUp() {
    parser = new GroovyAstParser();
  }

  @Nested
  @DisplayName("基本的なGroovyコードの解析")
  class BasicParsing {

    @Test
    @DisplayName("シンプルなクラス定義を解析できる")
    void parseSimpleClass() {
      // given
      String sourceCode =
          """
                class HelloWorld {
                    String message = "Hello, World!"

                    void sayHello() {
                        println message
                    }
                }
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("HelloWorld.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();

      GroovyAstParser.ParseResult parseResult = result.get();
      assertThat(parseResult.diagnostics()).isEmpty();

      List<ClassNode> classes = parseResult.getClasses();
      assertThat(classes).hasSize(1);

      ClassNode helloWorldClass = classes.get(0);
      assertThat(helloWorldClass.getName()).isEqualTo("HelloWorld");
      assertThat(helloWorldClass.getMethods())
          .anySatisfy(
              method -> {
                assertThat(method.getName()).isEqualTo("sayHello");
              });
    }

    @Test
    @DisplayName("Groovyスクリプトを解析できる")
    void parseGroovyScript() {
      // given
      String sourceCode =
          """
                def name = "Groovy"
                println "Hello, $name!"

                def add(a, b) {
                    a + b
                }

                println add(1, 2)
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("script.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      assertThat(result.get().diagnostics()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Spock特有の構文の解析")
  class SpockParsing {

    @Test
    @DisplayName("Spockテストクラスを解析できる")
    void parseSpockSpec() {
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

                    def "data driven test"() {
                        expect:
                        Math.max(a, b) == result

                        where:
                        a | b | result
                        1 | 2 | 2
                        3 | 1 | 3
                        5 | 5 | 5
                    }
                }
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("CalculatorSpec.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();

      GroovyAstParser.ParseResult parseResult = result.get();
      List<ClassNode> classes = parseResult.getClasses();
      assertThat(classes).hasSize(1);

      ClassNode specClass = classes.get(0);
      assertThat(specClass.getName()).isEqualTo("CalculatorSpec");

      // Spockのテストメソッドが正しく認識されているか確認
      List<MethodNode> methods = specClass.getMethods();
      assertThat(methods)
          .anySatisfy(
              method -> {
                assertThat(method.getName()).isEqualTo("addition should work correctly");
              });
      assertThat(methods)
          .anySatisfy(
              method -> {
                assertThat(method.getName()).isEqualTo("data driven test");
              });
    }
  }

  @Nested
  @DisplayName("エラー処理")
  class ErrorHandling {

    @Test
    @DisplayName("構文エラーがある場合でも部分的に解析できる")
    void parseWithSyntaxError() {
      // given
      String sourceCode =
          """
                class BrokenClass {
                    void method1() {
                        println "This is valid"
                    }

                    void method2() {
                        // 構文エラー: 閉じ括弧がない
                        println "Missing closing brace"

                    void method3() {
                        println "This method comes after error"
                    }
                }
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("BrokenClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();

      GroovyAstParser.ParseResult parseResult = result.get();
      assertThat(parseResult.diagnostics()).isNotEmpty();
      assertThat(parseResult.diagnostics())
          .anySatisfy(
              diagnostic -> {
                assertThat(diagnostic.severity())
                    .isEqualTo(GroovyAstParser.ParseDiagnostic.Severity.ERROR);
              });
    }

    @Test
    @DisplayName("不正なインポート文がある場合の診断")
    void parseWithInvalidImport() {
      // given
      String sourceCode =
          """
                import invalid.package.

                class TestClass {
                    void test() {
                        println "test"
                    }
                }
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("TestClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();

      GroovyAstParser.ParseResult parseResult = result.get();
      assertThat(parseResult.diagnostics()).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Groovy固有の機能")
  class GroovySpecificFeatures {

    @Test
    @DisplayName("クロージャを含むコードを解析できる")
    void parseClosures() {
      // given
      String sourceCode =
          """
                def list = [1, 2, 3, 4, 5]

                def evenNumbers = list.findAll { it % 2 == 0 }

                list.each { item ->
                    println "Item: $item"
                }

                def multiplier = { factor ->
                    return { number -> number * factor }
                }
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("closures.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      assertThat(result.get().diagnostics()).isEmpty();
    }

    @Test
    @DisplayName("GStringを含むコードを解析できる")
    void parseGStrings() {
      // given
      String sourceCode =
          """
                def name = "World"
                def greeting = "Hello, $name!"
                def multiline = \"\"\"
                    This is a multiline
                    GString with ${name}
                \"\"\"
                """;

      // when
      Either<GroovyAstParser.ParseError, GroovyAstParser.ParseResult> result =
          parser.parse("gstrings.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      assertThat(result.get().diagnostics()).isEmpty();
    }
  }
}
