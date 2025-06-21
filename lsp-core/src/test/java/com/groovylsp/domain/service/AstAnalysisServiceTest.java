package com.groovylsp.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.domain.model.ClassInfo;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.testing.FastTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** AstAnalysisServiceのテスト */
@FastTest
class AstAnalysisServiceTest {

  private AstAnalysisService service;
  private GroovyAstParser parser;

  @BeforeEach
  void setUp() {
    parser = new GroovyAstParser();
    service = new AstAnalysisService(parser);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (parser != null) {
      parser.close();
    }
  }

  @Nested
  @DisplayName("クラス定義の認識")
  class ClassRecognition {

    @Test
    @DisplayName("シンプルなクラス定義を認識できる")
    void recognizeSimpleClass() {
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
      var result = service.analyze("file:///test/HelloWorld.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.classes()).hasSize(1);
      var classInfo = astInfo.classes().get(0);
      assertThat(classInfo.name()).isEqualTo("HelloWorld");
      assertThat(classInfo.type()).isEqualTo(ClassInfo.ClassType.CLASS);
      assertThat(classInfo.fields()).hasSize(1);
      assertThat(classInfo.fields().get(0).name()).isEqualTo("message");
    }

    @Test
    @DisplayName("インターフェース定義を認識できる")
    void recognizeInterface() {
      // given
      String sourceCode =
          """
                interface Calculator {
                    int add(int a, int b)
                    int subtract(int a, int b)
                }
                """;

      // when
      var result = service.analyze("file:///test/Calculator.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.classes()).hasSize(1);
      var classInfo = astInfo.classes().get(0);
      assertThat(classInfo.name()).isEqualTo("Calculator");
      assertThat(classInfo.type()).isEqualTo(ClassInfo.ClassType.INTERFACE);
      assertThat(classInfo.methods()).hasSize(2);
    }

    @Test
    @DisplayName("複数のクラス定義を認識できる")
    void recognizeMultipleClasses() {
      // given
      String sourceCode =
          """
                class First {
                    void method1() {}
                }

                class Second {
                    void method2() {}
                }

                interface Third {
                    void method3()
                }
                """;

      // when
      var result = service.analyze("file:///test/Multiple.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.classes()).hasSize(3);
      assertThat(astInfo.classes().stream().map(ClassInfo::name))
          .containsExactlyInAnyOrder("First", "Second", "Third");
    }
  }

  @Nested
  @DisplayName("メソッド定義の認識")
  class MethodRecognition {

    @Test
    @DisplayName("メソッド定義を正しく認識できる")
    void recognizeMethods() {
      // given
      String sourceCode =
          """
                class MethodTest {
                    void noArgs() {
                        println "No arguments"
                    }

                    String withArgs(String name, int age) {
                        return "Name: $name, Age: $age"
                    }

                    private static int staticMethod(int x) {
                        return x * 2
                    }
                }
                """;

      // when
      var result = service.analyze("file:///test/MethodTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.classes()).hasSize(1);
      var methods = astInfo.classes().get(0).methods();

      // Groovyは自動的にコンストラクタやゲッター/セッターを生成することがある
      var userMethods =
          methods.stream()
              .filter(
                  m ->
                      m.name().equals("noArgs")
                          || m.name().equals("withArgs")
                          || m.name().equals("staticMethod"))
              .toList();

      assertThat(userMethods).hasSize(3);

      // noArgsメソッドの確認
      var noArgs = userMethods.stream().filter(m -> m.name().equals("noArgs")).findFirst();
      assertThat(noArgs).isPresent();
      assertThat(noArgs.get().parameters()).isEmpty();
      assertThat(noArgs.get().returnType()).isEqualTo("void");

      // withArgsメソッドの確認
      var withArgs = userMethods.stream().filter(m -> m.name().equals("withArgs")).findFirst();
      assertThat(withArgs).isPresent();
      assertThat(withArgs.get().parameters()).hasSize(2);
      assertThat(withArgs.get().parameters().get(0).name()).isEqualTo("name");
      assertThat(withArgs.get().parameters().get(0).type()).isEqualTo("String");
      assertThat(withArgs.get().parameters().get(1).name()).isEqualTo("age");
      assertThat(withArgs.get().parameters().get(1).type()).isEqualTo("int");
      assertThat(withArgs.get().returnType()).isEqualTo("String");

      // staticMethodの確認
      var staticMethod =
          userMethods.stream().filter(m -> m.name().equals("staticMethod")).findFirst();
      assertThat(staticMethod).isPresent();
      assertThat(staticMethod.get().isStatic()).isTrue();
      assertThat(staticMethod.get().isPrivate()).isTrue();
    }

    @Test
    @DisplayName("パッケージ付きクラスの完全修飾名が正しく生成される")
    void generateQualifiedNameWithPackage() {
      // given
      String sourceCode =
          """
                package com.example.test

                class TestClass {
                    String name
                }
                """;

      // when
      var result = service.analyze("file:///test/TestClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.packageName()).isEqualTo("com.example.test");
      assertThat(astInfo.classes()).hasSize(1);

      var classInfo = astInfo.classes().get(0);
      assertThat(classInfo.name()).isEqualTo("TestClass");
      assertThat(classInfo.qualifiedName()).isEqualTo("com.example.test.TestClass");
    }

    @Test
    @DisplayName("Spockテストメソッドを認識できる")
    void recognizeSpockMethods() {
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

                    def "subtraction should work"() {
                        expect:
                        5 - 3 == 2
                    }
                }
                """;

      // when
      var result = service.analyze("file:///test/CalculatorSpec.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.classes()).hasSize(1);
      var methods = astInfo.classes().get(0).methods();

      // Spockのテストメソッドを確認
      var testMethods = methods.stream().filter(m -> m.name().contains("should")).toList();

      assertThat(testMethods).hasSize(2);
      assertThat(testMethods.stream().map(m -> m.name()))
          .containsExactlyInAnyOrder("addition should work correctly", "subtraction should work");
    }
  }

  @Nested
  @DisplayName("構文エラーの検出")
  class SyntaxErrorDetection {

    @Test
    @DisplayName("構文エラーを検出できる")
    void detectSyntaxErrors() {
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
      var result = service.analyze("file:///test/BrokenClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.hasSyntaxErrors()).isTrue();
      assertThat(astInfo.syntaxErrors()).isNotEmpty();

      var syntaxError = astInfo.syntaxErrors().get(0);
      assertThat(syntaxError.severity()).isEqualTo(DiagnosticItem.DiagnosticSeverity.ERROR);
      assertThat(syntaxError.source()).isEqualTo("groovy-syntax");
    }

    @Test
    @DisplayName("不正なインポート文を検出できる")
    void detectInvalidImport() {
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
      var result = service.analyze("file:///test/TestClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.hasSyntaxErrors()).isTrue();
      assertThat(astInfo.syntaxErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("エラーがない場合は空のリストを返す")
    void noErrorsForValidCode() {
      // given
      String sourceCode =
          """
                class ValidClass {
                    String name
                    int age

                    void display() {
                        println "Name: $name, Age: $age"
                    }
                }
                """;

      // when
      var result = service.analyze("file:///test/ValidClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.hasSyntaxErrors()).isFalse();
      assertThat(astInfo.syntaxErrors()).isEmpty();
    }
  }

  @Nested
  @DisplayName("パッケージとインポートの認識")
  class PackageAndImportRecognition {

    @Test
    @DisplayName("パッケージ宣言を認識できる")
    void recognizePackage() {
      // given
      String sourceCode =
          """
                package com.example.test

                class TestClass {
                    void test() {}
                }
                """;

      // when
      var result = service.analyze("file:///test/TestClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.packageName()).isEqualTo("com.example.test");
    }

    @Test
    @DisplayName("インポート文を認識できる")
    void recognizeImports() {
      // given
      String sourceCode =
          """
                package com.example

                import java.util.List
                import java.util.Map
                import groovy.transform.CompileStatic

                @CompileStatic
                class TestClass {
                    List<String> names
                    Map<String, Integer> scores
                }
                """;

      // when
      var result = service.analyze("file:///test/TestClass.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      var astInfo = result.get();

      assertThat(astInfo.imports())
          .extracting(imp -> imp.className())
          .containsExactlyInAnyOrder(
              "java.util.List", "java.util.Map", "groovy.transform.CompileStatic");
    }
  }
}
