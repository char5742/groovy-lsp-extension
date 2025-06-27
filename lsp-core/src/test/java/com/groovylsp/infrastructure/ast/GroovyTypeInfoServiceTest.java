package com.groovylsp.infrastructure.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.infrastructure.documentation.GroovyDocumentationService;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GroovyTypeInfoServiceのテスト */
@FastTest
class GroovyTypeInfoServiceTest {

  private GroovyTypeInfoService service;
  private SymbolTable symbolTable;
  private ScopeManager scopeManager;
  private DocumentContentService documentContentService;

  @BeforeEach
  void setUp() {
    var parser = new GroovyAstParser();
    symbolTable = new SymbolTable();
    scopeManager = new ScopeManager();
    TextDocumentRepository repository = new InMemoryTextDocumentRepository();
    documentContentService = new DocumentContentService(repository);
    var astAnalysisService = new AstAnalysisService(parser);
    var documentationService = new GroovyDocumentationService();
    service =
        new GroovyTypeInfoService(
            parser, symbolTable, scopeManager, documentContentService, astAnalysisService, documentationService);
  }

  @Test
  void ローカル変数の型情報を取得できる() {
    // given
    String content =
        """
        def main() {
            String name = "test"
            int age = 20
            def value = 123
        }
        """;

    // when - String型の変数にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 11)); // "name"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("name", typeInfo.name());
    assertEquals("String", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.LOCAL_VARIABLE, typeInfo.kind());
  }

  @Test
  void プリミティブ型の変数の型情報を取得できる() {
    // given
    String content =
        """
        def main() {
            int count = 10
            double price = 99.99
            boolean active = true
        }
        """;

    // when - int型の変数にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 8)); // "count"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("count", typeInfo.name());
    assertEquals("Integer", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.LOCAL_VARIABLE, typeInfo.kind());
  }

  @Test
  void フィールドの型情報を取得できる() {
    // given
    String content =
        """
        class Person {
            private String name
            int age
            public static final String CONSTANT = "VALUE"
        }
        """;

    // when - private String型のフィールドにカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 19)); // "name"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("name", typeInfo.name());
    assertEquals("String", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.FIELD, typeInfo.kind());
    assertNotNull(typeInfo.modifiers());
    assertTrue(typeInfo.modifiers().contains("private"));
  }

  @Test
  void メソッドパラメータの型情報を取得できる() {
    // given
    String content =
        """
        class Calculator {
            int add(int a, int b) {
                return a + b
            }
        }
        """;

    // when - パラメータ "a" にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 16)); // パラメータ "a" の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("a", typeInfo.name());
    assertEquals("int", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.PARAMETER, typeInfo.kind());
  }

  @Test
  void メソッドの型情報を取得できる() {
    // given
    String content =
        """
        class Calculator {
            int add(int a, int b) {
                return a + b
            }
        }
        """;

    // when - メソッド名 "add" にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 8)); // "add"の位置（0ベース）

    // then
    if (result.isLeft()) {
      System.err.println("メソッドテストエラー: " + result.getLeft());
    }
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("add", typeInfo.name());
    // Groovyではintパラメータはintのまま
    assertTrue(typeInfo.type().contains("add(int a, int b): int"));
    assertEquals(TypeInfoService.TypeInfo.Kind.METHOD, typeInfo.kind());
  }

  @Test
  void クラスの型情報を取得できる() {
    // given
    String content = """
        class Person {
            String name
        }
        """;

    // when - クラス名 "Person" にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(0, 6)); // "Person"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("Person", typeInfo.name());
    assertEquals("Person", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.CLASS, typeInfo.kind());
  }

  @Test
  void 配列型の変数の型情報を取得できる() {
    // given
    String content =
        """
        def main() {
            String[] names = new String[10]
            int[][] matrix = [[1, 2], [3, 4]]
        }
        """;

    // when - String配列型の変数にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 13)); // "names"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("names", typeInfo.name());
    assertEquals("String[]", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.LOCAL_VARIABLE, typeInfo.kind());
  }

  @Test
  void ジェネリクス型の変数の型情報を取得できる() {
    // given
    String content =
        """
        import java.util.List
        import java.util.Map

        def main() {
            List<String> items = []
            Map<String, Integer> counts = [:]
        }
        """;

    // when - List<String>型の変数にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(4, 17)); // "items"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("items", typeInfo.name());
    assertEquals("List<String>", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.LOCAL_VARIABLE, typeInfo.kind());
  }

  @Test
  void 初期値付きフィールドの型情報を取得できる() {
    // given
    String content =
        """
        class Person {
            private String name = "John"
            int age = 25
            List<String> hobbies = ["reading", "swimming"]
        }
        """;

    // when - private String型のフィールドにカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 19)); // "name"の位置（0ベース）

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("name", typeInfo.name());
    System.out.println("Name field type: " + typeInfo.type());
    assertEquals("String", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.FIELD, typeInfo.kind());
    assertNotNull(typeInfo.modifiers());
    assertTrue(typeInfo.modifiers().contains("private"));

    // intフィールドの確認
    result = service.getTypeInfoAt("test.groovy", content, new Position(2, 8)); // "age"の位置
    assertTrue(result.isRight());
    typeInfo = result.get();
    assertEquals("age", typeInfo.name());
    assertEquals("int", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.FIELD, typeInfo.kind());

    // List<String>フィールドの確認
    result = service.getTypeInfoAt("test.groovy", content, new Position(3, 21)); // "hobbies"の位置
    assertTrue(result.isRight());
    typeInfo = result.get();
    assertEquals("hobbies", typeInfo.name());
    // Groovy ASTが返す型情報を確認
    System.out.println("Hobbies type: " + typeInfo.type());
    assertTrue(typeInfo.type().contains("List") || typeInfo.type().contains("ArrayList"));
    assertEquals(TypeInfoService.TypeInfo.Kind.FIELD, typeInfo.kind());
  }

  @Test
  void 型情報が見つからない場合はエラーを返す() {
    // given
    String content = """
        def main() {
            // コメント
        }
        """;

    // when - コメント部分にカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(1, 8));

    // then
    assertTrue(result.isLeft());
    assertTrue(result.getLeft().contains("識別子") || result.getLeft().contains("型情報"));
  }

  @Test
  void 無効な構文の場合はパースエラーを返す() {
    // given
    String content = "class { invalid syntax";

    // when
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(0, 6));

    // then
    assertTrue(result.isLeft());
    String errorMessage = result.getLeft();
    // 無効な構文の場合、パーサーはモジュールノードを生成できない
    assertTrue(errorMessage.contains("モジュールノードが見つかりません") || errorMessage.contains("パースエラー"));
  }

  @Test
  void インポート文の型情報を取得できる() {
    // given
    String content =
        """
        import java.util.List
        import java.util.Map

        class Test {
            List<String> items
        }
        """;

    // when - import文のListにカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(0, 18)); // "List"の位置（0ベース）

    // then
    // 現在の実装ではインポート文は未サポート
    assertTrue(result.isLeft());
  }

  @Test
  void メソッド呼び出しの型情報を取得できる() {
    // given
    String content =
        """
        class Calculator {
            int add(int a, int b) {
                return a + b
            }

            def test() {
                int result = add(5, 3)
                return result
            }
        }
        """;

    // when - メソッド呼び出しのaddにカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(6, 22)); // "add"の呼び出し位置

    // then
    if (result.isLeft()) {
      System.err.println("メソッド呼び出しテストエラー: " + result.getLeft());
    }
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("add", typeInfo.name());
    assertEquals(TypeInfoService.TypeInfo.Kind.METHOD, typeInfo.kind());
  }

  @Test
  void クロージャの型情報を取得できる() {
    // given
    String content =
        """
        def closure = { String name ->
            println "Hello, $name"
        }
        """;

    // when - クロージャパラメータのnameにカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(0, 23)); // "name"の位置

    // then
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("name", typeInfo.name());
    assertEquals("String", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.PARAMETER, typeInfo.kind());
    assertEquals("クロージャパラメータ", typeInfo.documentation());
  }

  @Test
  void プロパティアクセスの型情報を取得できる() {
    // given
    String content =
        """
        class Person {
            String name

            def test() {
                this.name = "John"
            }
        }
        """;

    // when - this.nameのnameにカーソルを合わせる
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(4, 13)); // "name"の位置

    // then
    if (result.isLeft()) {
      System.err.println("プロパティアクセステストエラー: " + result.getLeft());
    }
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("name", typeInfo.name());
    assertEquals("String", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.FIELD, typeInfo.kind());
  }

  @Test
  void コンストラクタの型情報を取得できる() {
    // given
    String content =
        """
        class Person {
            String name

            Person(String name) {
                this.name = name
            }
        }
        """;

    // コンテンツを行ごとに確認
    String[] lines = content.split("\n");
    for (int i = 0; i < lines.length; i++) {
      System.out.println("行 " + i + ": '" + lines[i] + "'");
    }

    // when - コンストラクタ名にカーソルを合わせる（行3の'    Person(String name) {'のPersonのPの位置）
    Either<String, TypeInfoService.TypeInfo> result =
        service.getTypeInfoAt("test.groovy", content, new Position(3, 8)); // "Person"コンストラクタの位置

    // then
    if (result.isLeft()) {
      System.err.println("コンストラクタテストエラー: " + result.getLeft());
    }
    assertTrue(result.isRight());
    TypeInfoService.TypeInfo typeInfo = result.get();
    assertEquals("Person", typeInfo.name());
    assertEquals("Person(String name)", typeInfo.type());
    assertEquals(TypeInfoService.TypeInfo.Kind.METHOD, typeInfo.kind());
    assertEquals("コンストラクタ", typeInfo.documentation());
  }
}
