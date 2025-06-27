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
                assertThat(symbol.detail()).isEqualTo(": String");
              })
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("age");
                assertThat(symbol.detail()).isEqualTo(": int");
              })
          .anySatisfy(
              symbol -> {
                assertThat(symbol.name()).isEqualTo("address");
                assertThat(symbol.detail()).isEqualTo(": String");
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
                // 明示的にObjectを継承している場合は表示される
                assertThat(symbol.detail()).isEqualTo(": extends Object implements Runnable");
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

  @Nested
  @DisplayName("フィールドの型推論")
  class FieldTypeInference {

    @Test
    @DisplayName("defで宣言されたフィールドの型が初期化式から推論される")
    void inferTypeFromInitialExpression() {
      // given
      String sourceCode =
          """
          class Person {
              def name = "John"
              def age = 25
              def height = 180.5
              def isActive = true
              def scores = [90, 85, 92]
              def nullValue = null
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

      // 各フィールドの型が正しく推論されているか確認
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("name");
                assertThat(field.kind()).isEqualTo(SymbolKind.Property);
                assertThat(field.detail()).isEqualTo(": String");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("age");
                assertThat(field.kind()).isEqualTo(SymbolKind.Property);
                assertThat(field.detail()).isEqualTo(": int");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("height");
                assertThat(field.kind()).isEqualTo(SymbolKind.Property);
                // 180.5のような小数リテラルはGroovyではBigDecimalとして扱われる
                assertThat(field.detail()).isIn(": double", ": float", ": BigDecimal");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("isActive");
                assertThat(field.kind()).isEqualTo(SymbolKind.Property);
                assertThat(field.detail()).isEqualTo(": boolean");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("scores");
                assertThat(field.kind()).isEqualTo(SymbolKind.Property);
                // リストリテラルはArrayList型として推論される
                assertThat(field.detail()).isEqualTo(": ArrayList");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("nullValue");
                assertThat(field.kind()).isEqualTo(SymbolKind.Property);
                // nullの場合はObject型になる
                assertThat(field.detail()).isEqualTo(": Object");
              });
    }

    @Test
    @DisplayName("コンストラクタ呼び出しから型が推論される")
    void inferTypeFromConstructorCall() {
      // given
      String sourceCode =
          """
          class Service {
              def user = new User()
              def list = new ArrayList<String>()
              def map = new HashMap<String, Integer>()
          }

          class User {
              String name
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/Service.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();

      // Serviceクラスのシンボルを検証
      Symbol serviceClass =
          symbols.stream().filter(s -> s.name().equals("Service")).findFirst().orElseThrow();

      assertThat(serviceClass.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("user");
                assertThat(field.detail()).isEqualTo(": User");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("list");
                assertThat(field.detail()).isEqualTo(": ArrayList");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("map");
                assertThat(field.detail()).isEqualTo(": HashMap");
              });
    }

    @Test
    @DisplayName("Spockのモック生成メソッドから型が推論される")
    void inferTypeFromSpockMocks() {
      // given
      String sourceCode =
          """
          import spock.lang.Specification

          class ServiceSpec extends Specification {
              def userService = Mock(UserService)
              def repository = Stub(UserRepository)
              def cache = Spy(CacheService)
          }

          interface UserService {}
          interface UserRepository {}
          class CacheService {}
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/ServiceSpec.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();

      // ServiceSpecクラスのシンボルを検証
      Symbol specClass =
          symbols.stream().filter(s -> s.name().equals("ServiceSpec")).findFirst().orElseThrow();

      assertThat(specClass.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("userService");
                assertThat(field.detail()).isEqualTo(": UserService");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("repository");
                assertThat(field.detail()).isEqualTo(": UserRepository");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("cache");
                assertThat(field.detail()).isEqualTo(": CacheService");
              });
    }

    @Test
    @DisplayName("明示的に型が宣言されたフィールドはその型を維持する")
    void keepExplicitlyDeclaredTypes() {
      // given
      String sourceCode =
          """
          class TypedFields {
              String explicitString = "value"
              int explicitInt = 42
              List<String> explicitList = ["a", "b"]
              Object explicitObject = "still object"
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/TypedFields.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("explicitString");
                assertThat(field.detail()).isEqualTo(": String");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("explicitInt");
                assertThat(field.detail()).isEqualTo(": int");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("explicitList");
                assertThat(field.detail()).isEqualTo(": List");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("explicitObject");
                // 明示的にObjectと宣言されている場合はObjectのまま
                assertThat(field.detail()).isEqualTo(": Object");
              });
    }
  }

  @Nested
  @DisplayName("クラス解決キャッシュ機能")
  class ClassResolutionCache {

    @Test
    @DisplayName("複数のスターインポートでのクラス解決でキャッシュが効く")
    void cacheWorksForStarImports() {
      // given
      String sourceCode =
          """
          import java.util.*
          import java.nio.file.*

          class ImportTest {
              def list1 = Mock(List)
              def list2 = Mock(List)  // 同じクラス名の2回目
              def map1 = Mock(Map)
              def map2 = Mock(Map)    // 同じクラス名の2回目
              def path1 = Mock(Path)
              def path2 = Mock(Path)  // 同じクラス名の2回目
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/ImportTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.name()).isEqualTo("ImportTest");

      // 各フィールドが正しく解決されているか確認（キャッシュが効いていても結果は同じ）
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("list1");
                assertThat(field.detail()).isEqualTo(": List");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("list2");
                assertThat(field.detail()).isEqualTo(": List");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("map1");
                assertThat(field.detail()).isEqualTo(": Map");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("map2");
                assertThat(field.detail()).isEqualTo(": Map");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("path1");
                assertThat(field.detail()).isEqualTo(": Path");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("path2");
                assertThat(field.detail()).isEqualTo(": Path");
              });
    }

    @Test
    @DisplayName("デフォルトパッケージのクラス解決でキャッシュが効く")
    void cacheWorksForDefaultPackages() {
      // given
      String sourceCode =
          """
          class DefaultPackageTest {
              def str1 = Mock(String)
              def str2 = Mock(String)     // 同じクラス名の2回目（java.lang.String）
              def obj1 = Mock(Object)
              def obj2 = Mock(Object)     // 同じクラス名の2回目（java.lang.Object）
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/DefaultPackageTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.name()).isEqualTo("DefaultPackageTest");

      // 各フィールドが正しく解決されているか確認
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("str1");
                assertThat(field.detail()).isEqualTo(": String");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("str2");
                assertThat(field.detail()).isEqualTo(": String");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("obj1");
                assertThat(field.detail()).isEqualTo(": Object");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("obj2");
                assertThat(field.detail()).isEqualTo(": Object");
              });
    }

    @Test
    @DisplayName("存在しないクラス名の解決も適切にキャッシュされる")
    void cacheWorksForNonExistentClasses() {
      // given
      String sourceCode =
          """
          import java.util.*

          class NonExistentClassTest {
              def unknown1 = Mock(NonExistentClass)
              def unknown2 = Mock(NonExistentClass)  // 同じ存在しないクラス名の2回目
              def another1 = Mock(AnotherMissingClass)
              def another2 = Mock(AnotherMissingClass)  // 同じ存在しないクラス名の2回目
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/NonExistentClassTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.name()).isEqualTo("NonExistentClassTest");

      // 存在しないクラス名でも、フォールバック処理により ClassNode が作成されるはず
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("unknown1");
                assertThat(field.detail()).isEqualTo(": NonExistentClass");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("unknown2");
                assertThat(field.detail()).isEqualTo(": NonExistentClass");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("another1");
                assertThat(field.detail()).isEqualTo(": AnotherMissingClass");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("another2");
                assertThat(field.detail()).isEqualTo(": AnotherMissingClass");
              });
    }

    @Test
    @DisplayName("キャッシュクリア機能が正しく動作する")
    void cacheClearWorksCorrectly() {
      // given
      String sourceCode =
          """
          import java.util.*

          class CacheClearTest {
              def list = Mock(List)
              def map = Mock(Map)
          }
          """;

      // when - 1回目の解析
      Either<String, List<Symbol>> result1 =
          service.extractSymbols("file:///test/CacheClearTest.groovy", sourceCode);

      // キャッシュをクリア
      service.clearCache();

      // 2回目の解析
      Either<String, List<Symbol>> result2 =
          service.extractSymbols("file:///test/CacheClearTest.groovy", sourceCode);

      // then - 両方とも正しく解析される
      assertThat(result1.isRight()).isTrue();
      assertThat(result2.isRight()).isTrue();

      List<Symbol> symbols1 = result1.get();
      List<Symbol> symbols2 = result2.get();

      // 同じ結果が得られることを確認
      assertThat(symbols1).hasSize(1);
      assertThat(symbols2).hasSize(1);

      assertThat(symbols1.get(0).children()).hasSize(2);
      assertThat(symbols2.get(0).children()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("名前解決の優先順位")
  class NameResolutionPriority {

    @Test
    @DisplayName("明示的インポートがスターインポートより優先される")
    void explicitImportTakesPrecedence() {
      // given
      String sourceCode =
          """
          import java.util.Date
          import java.sql.*

          class ImportPriorityTest {
              def date = Mock(Date)  // java.util.Dateが使われるべき
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/ImportPriorityTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("date");
                // java.util.Dateが優先されることを確認
                assertThat(field.detail()).isEqualTo(": Date");
              });
    }

    @Test
    @DisplayName("スターインポートがデフォルトインポートより優先される")
    void starImportTakesPrecedenceOverDefault() {
      // given
      String sourceCode =
          """
          import com.example.*

          class StarImportTest {
              def list = Mock(List)  // java.util.Listが使われる（デフォルト）
              def customList = Mock(CustomList)  // com.example.CustomListは存在しないため解決されない
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/StarImportTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("list");
                assertThat(field.detail()).isEqualTo(": List");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("customList");
                // 存在しないクラスでもフォールバックで作成される
                assertThat(field.detail()).isEqualTo(": CustomList");
              });
    }

    @Test
    @DisplayName("同じパッケージのクラスが優先的に解決される")
    void samePackageClassResolution() {
      // given
      String sourceCode =
          """
          package com.example

          import java.util.*

          class SamePackageTest {
              def helper = Mock(Helper)  // 同じパッケージのHelperクラスを想定
              def list = Mock(List)      // java.util.List
          }

          class Helper {
              String name
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/SamePackageTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();

      // デバッグ用：実際に返されたシンボルを確認
      assertThat(symbols).hasSizeGreaterThanOrEqualTo(1);

      // SamePackageTestクラスを検証
      Symbol testClass =
          symbols.stream()
              .filter(s -> s.name().equals("com.example.SamePackageTest"))
              .findFirst()
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "com.example.SamePackageTest not found. Found symbols: "
                              + symbols.stream().map(Symbol::name).toList()));

      assertThat(testClass.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("helper");
                assertThat(field.detail()).isEqualTo(": Helper");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("list");
                assertThat(field.detail()).isEqualTo(": List");
              });
    }

    @Test
    @DisplayName("Groovyのデフォルトインポートが機能する")
    void groovyDefaultImports() {
      // given
      String sourceCode =
          """
          class GroovyDefaultTest {
              def bigInt = Mock(BigInteger)    // java.math.BigInteger (java.lang以外)
              def bigDec = Mock(BigDecimal)    // java.math.BigDecimal
              def file = Mock(File)            // java.io.File
              def url = Mock(URL)              // java.net.URL
          }
          """;

      // when
      Either<String, List<Symbol>> result =
          service.extractSymbols("file:///test/GroovyDefaultTest.groovy", sourceCode);

      // then
      assertThat(result.isRight()).isTrue();
      List<Symbol> symbols = result.get();
      assertThat(symbols).hasSize(1);

      Symbol classSymbol = symbols.get(0);
      assertThat(classSymbol.children())
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("bigInt");
                assertThat(field.detail()).isEqualTo(": BigInteger");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("bigDec");
                assertThat(field.detail()).isEqualTo(": BigDecimal");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("file");
                assertThat(field.detail()).isEqualTo(": File");
              })
          .anySatisfy(
              field -> {
                assertThat(field.name()).isEqualTo("url");
                assertThat(field.detail()).isEqualTo(": URL");
              });
    }
  }
}
