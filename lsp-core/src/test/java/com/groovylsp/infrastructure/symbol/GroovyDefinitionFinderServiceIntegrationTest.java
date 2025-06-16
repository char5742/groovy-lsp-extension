package com.groovylsp.infrastructure.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.ClassInfo;
import com.groovylsp.domain.model.FieldInfo;
import com.groovylsp.domain.model.MethodInfo;
import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolDefinition;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.testing.IntegrationTest;
import io.vavr.collection.List;
import io.vavr.control.Either;
import java.net.URI;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GroovyDefinitionFinderServiceの統合テスト */
@IntegrationTest
class GroovyDefinitionFinderServiceIntegrationTest {

  private GroovyDefinitionFinderService definitionFinder;
  private SymbolTable symbolTable;
  private ScopeManager scopeManager;
  private TextDocumentRepository repository;
  private GroovySymbolTableBuilderService symbolTableBuilder;

  @BeforeEach
  void setUp() {
    symbolTable = new SymbolTable();
    scopeManager = new ScopeManager();
    repository = new InMemoryTextDocumentRepository();
    var documentContentService = new DocumentContentService(repository);
    definitionFinder =
        new GroovyDefinitionFinderService(symbolTable, scopeManager, documentContentService);
    symbolTableBuilder = new GroovySymbolTableBuilderService();
  }

  @Test
  void testFindDefinitionForMethod() {
    // Given
    String uri = "file:///test.groovy";
    String content =
        """
        package com.example

        class TestClass {
            String testField

            String testMethod(String param) {
                return param
            }

            void caller() {
                testMethod("hello")
            }
        }
        """;

    // ドキュメントを保存
    repository.save(new TextDocument(URI.create(uri), "groovy", 1, content));

    // AST情報を作成
    var classPos = new ClassInfo.Position(3, 1, 14, 1);
    var fieldPos = new ClassInfo.Position(4, 5, 4, 20);
    var methodPos = new ClassInfo.Position(6, 5, 8, 5);
    var callerPos = new ClassInfo.Position(10, 5, 12, 5);

    var fieldInfo =
        new FieldInfo(
            "testField", "String", fieldPos, java.lang.reflect.Modifier.PRIVATE, null, null);

    var methodInfo =
        new MethodInfo(
            "testMethod",
            "String",
            java.util.List.of(new MethodInfo.ParameterInfo("param", "String", null, false)),
            methodPos,
            java.lang.reflect.Modifier.PUBLIC,
            null);

    var callerInfo =
        new MethodInfo(
            "caller",
            "void",
            java.util.List.of(),
            callerPos,
            java.lang.reflect.Modifier.PUBLIC,
            null);

    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            java.util.List.of(methodInfo, callerInfo),
            java.util.List.of(fieldInfo),
            java.util.List.of(),
            java.util.List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo =
        new AstInfo(
            uri,
            java.util.List.of(classInfo),
            java.util.List.of(),
            "com.example",
            java.util.List.of());

    // シンボルテーブルとスコープを構築
    symbolTableBuilder.buildSymbolTable(astInfo, uri, symbolTable);
    Either<String, com.groovylsp.domain.model.Scope> scopeResult =
        symbolTableBuilder.buildScope(astInfo, uri);
    assertTrue(scopeResult.isRight());
    scopeManager.setRootScope(uri, scopeResult.get());

    // When - testMethodの呼び出し位置（11行目、8列目）から定義を検索
    var callPosition = new Position(10, 8); // 0ベース
    Either<String, List<SymbolDefinition>> result =
        definitionFinder.findDefinition(uri, callPosition);

    // Then
    assertTrue(result.isRight());
    List<SymbolDefinition> definitions = result.get();
    assertEquals(1, definitions.size());

    SymbolDefinition def = definitions.get(0);
    assertEquals("testMethod", def.name());
    assertEquals("com.example.TestClass.testMethod", def.qualifiedName());
    assertEquals(SymbolDefinition.DefinitionType.METHOD, def.definitionType());
    assertEquals(5, def.range().getStart().getLine()); // 0ベースで5（元の6行目）
  }

  @Test
  void testFindDefinitionByQualifiedName() {
    // Given
    String uri = "file:///test.groovy";
    var classPos = new ClassInfo.Position(1, 1, 10, 1);

    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo =
        new AstInfo(
            uri,
            java.util.List.of(classInfo),
            java.util.List.of(),
            "com.example",
            java.util.List.of());

    // シンボルテーブルを構築
    symbolTableBuilder.buildSymbolTable(astInfo, uri, symbolTable);

    // When
    Either<String, SymbolDefinition> result =
        definitionFinder.findDefinitionByQualifiedName("com.example.TestClass");

    // Then
    assertTrue(result.isRight());
    SymbolDefinition def = result.get();
    assertEquals("TestClass", def.name());
    assertEquals("com.example.TestClass", def.qualifiedName());
  }

  @Test
  void testFindDefinitionWithScope() {
    // Given
    String uri = "file:///test.groovy";
    String content =
        """
        package com.example

        class TestClass {
            String outerVar = "outer"

            void testMethod() {
                String innerVar = "inner"
                println innerVar
                println outerVar
            }
        }
        """;

    repository.save(new TextDocument(URI.create(uri), "groovy", 1, content));

    // AST情報を作成（簡略化）
    var classPos = new ClassInfo.Position(3, 1, 11, 1);
    var fieldPos = new ClassInfo.Position(4, 5, 4, 30);
    var methodPos = new ClassInfo.Position(6, 5, 10, 5);

    var fieldInfo =
        new FieldInfo(
            "outerVar", "String", fieldPos, java.lang.reflect.Modifier.PRIVATE, "\"outer\"", null);

    var methodInfo =
        new MethodInfo(
            "testMethod",
            "void",
            java.util.List.of(),
            methodPos,
            java.lang.reflect.Modifier.PUBLIC,
            null);

    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            java.util.List.of(methodInfo),
            java.util.List.of(fieldInfo),
            java.util.List.of(),
            java.util.List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo =
        new AstInfo(
            uri,
            java.util.List.of(classInfo),
            java.util.List.of(),
            "com.example",
            java.util.List.of());

    // シンボルテーブルとスコープを構築
    symbolTableBuilder.buildSymbolTable(astInfo, uri, symbolTable);
    Either<String, com.groovylsp.domain.model.Scope> scopeResult =
        symbolTableBuilder.buildScope(astInfo, uri);
    assertTrue(scopeResult.isRight());
    scopeManager.setRootScope(uri, scopeResult.get());

    // When - outerVarの参照位置（9行目）から定義を検索
    var refPosition = new Position(8, 16); // 0ベース
    Either<String, List<SymbolDefinition>> result =
        definitionFinder.findDefinitionByName("outerVar", uri, refPosition);

    // Then
    assertTrue(result.isRight());
    List<SymbolDefinition> definitions = result.get();
    assertEquals(1, definitions.size());

    SymbolDefinition def = definitions.get(0);
    assertEquals("outerVar", def.name());
    assertEquals("com.example.TestClass.outerVar", def.qualifiedName());
    assertEquals(SymbolDefinition.DefinitionType.FIELD, def.definitionType());
  }

  @Test
  void testFindDefinitionNotFound() {
    // Given
    String uri = "file:///test.groovy";
    String content = "println unknownVar";
    repository.save(new TextDocument(URI.create(uri), "groovy", 1, content));

    // When
    var position = new Position(0, 8);
    Either<String, List<SymbolDefinition>> result = definitionFinder.findDefinition(uri, position);

    // Then
    assertTrue(result.isRight());
    assertTrue(result.get().isEmpty());
  }
}
