package com.groovylsp.infrastructure.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.ClassInfo;
import com.groovylsp.domain.model.FieldInfo;
import com.groovylsp.domain.model.MethodInfo;
import com.groovylsp.domain.model.Scope;
import com.groovylsp.domain.model.SymbolDefinition;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GroovySymbolTableBuilderServiceの単体テスト */
@FastTest
class GroovySymbolTableBuilderServiceTest {

  private GroovySymbolTableBuilderService service;
  private SymbolTable symbolTable;

  @BeforeEach
  void setUp() {
    service = new GroovySymbolTableBuilderService();
    symbolTable = new SymbolTable();
  }

  @Test
  void testBuildSymbolTableWithClass() {
    // Given
    String uri = "file:///test.groovy";
    var classPos = new ClassInfo.Position(1, 1, 10, 1);
    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo = new AstInfo(uri, List.of(classInfo), List.of(), "com.example", List.of());

    // When
    Either<String, Void> result = service.buildSymbolTable(astInfo, uri, symbolTable);

    // Then
    assertTrue(result.isRight());

    Option<SymbolDefinition> found = symbolTable.findByQualifiedName("com.example.TestClass");
    assertTrue(found.isDefined());
    assertEquals("TestClass", found.get().name());
    assertEquals(SymbolDefinition.DefinitionType.CLASS, found.get().definitionType());
  }

  @Test
  void testBuildSymbolTableWithMethods() {
    // Given
    String uri = "file:///test.groovy";
    var classPos = new ClassInfo.Position(1, 1, 20, 1);
    var methodPos = new ClassInfo.Position(5, 5, 10, 5);

    var methodInfo =
        new MethodInfo(
            "testMethod",
            "String",
            List.of(new MethodInfo.ParameterInfo("param1", "String", null, false)),
            methodPos,
            java.lang.reflect.Modifier.PUBLIC,
            null);

    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            List.of(methodInfo),
            List.of(),
            List.of(),
            List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo = new AstInfo(uri, List.of(classInfo), List.of(), "com.example", List.of());

    // When
    Either<String, Void> result = service.buildSymbolTable(astInfo, uri, symbolTable);

    // Then
    assertTrue(result.isRight());

    Option<SymbolDefinition> methodDef =
        symbolTable.findByQualifiedName("com.example.TestClass.testMethod");
    assertTrue(methodDef.isDefined());
    assertEquals("testMethod", methodDef.get().name());
    assertEquals(SymbolDefinition.DefinitionType.METHOD, methodDef.get().definitionType());
    assertEquals("com.example.TestClass", methodDef.get().containingClass());
  }

  @Test
  void testBuildSymbolTableWithFields() {
    // Given
    String uri = "file:///test.groovy";
    var classPos = new ClassInfo.Position(1, 1, 20, 1);
    var fieldPos = new ClassInfo.Position(3, 5, 3, 20);

    var fieldInfo =
        new FieldInfo(
            "testField", "String", fieldPos, java.lang.reflect.Modifier.PRIVATE, null, null);

    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            List.of(),
            List.of(fieldInfo),
            List.of(),
            List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo = new AstInfo(uri, List.of(classInfo), List.of(), "com.example", List.of());

    // When
    Either<String, Void> result = service.buildSymbolTable(astInfo, uri, symbolTable);

    // Then
    assertTrue(result.isRight());

    Option<SymbolDefinition> fieldDef =
        symbolTable.findByQualifiedName("com.example.TestClass.testField");
    assertTrue(fieldDef.isDefined());
    assertEquals("testField", fieldDef.get().name());
    assertEquals(SymbolDefinition.DefinitionType.FIELD, fieldDef.get().definitionType());
  }

  @Test
  void testBuildScope() {
    // Given
    String uri = "file:///test.groovy";
    var classPos = new ClassInfo.Position(5, 1, 15, 1);
    var methodPos = new ClassInfo.Position(7, 5, 10, 5);
    var fieldPos = new ClassInfo.Position(6, 5, 6, 20);

    var fieldInfo =
        new FieldInfo(
            "testField", "String", fieldPos, java.lang.reflect.Modifier.PRIVATE, null, null);

    var methodInfo =
        new MethodInfo(
            "testMethod",
            "String",
            List.of(
                new MethodInfo.ParameterInfo("param1", "String", null, false),
                new MethodInfo.ParameterInfo("param2", "int", null, false)),
            methodPos,
            java.lang.reflect.Modifier.PUBLIC,
            null);

    var classInfo =
        new ClassInfo(
            "TestClass",
            "com.example.TestClass",
            ClassInfo.ClassType.CLASS,
            classPos,
            List.of(methodInfo),
            List.of(fieldInfo),
            List.of(),
            List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo = new AstInfo(uri, List.of(classInfo), List.of(), "com.example", List.of());

    // When
    Either<String, Scope> result = service.buildScope(astInfo, uri);

    // Then
    assertTrue(result.isRight());
    Scope rootScope = result.get();

    // ルートスコープの検証
    assertEquals(Scope.ScopeType.GLOBAL, rootScope.getType());
    assertNull(rootScope.getParent());

    // クラスがルートスコープに追加されているか確認
    Option<SymbolDefinition> classDef = rootScope.findSymbol("TestClass");
    assertTrue(classDef.isDefined());

    // ルートスコープの子スコープを確認
    io.vavr.collection.List<Scope> rootChildren = rootScope.getChildren();
    assertEquals(1, rootChildren.size(), "ルートスコープに子スコープが1つあるはずです");

    // 子スコープを直接取得して検証
    Scope directClassScope = rootChildren.get(0);
    assertEquals(Scope.ScopeType.CLASS, directClassScope.getType());
    assertEquals("com.example.TestClass", directClassScope.getName());

    // クラススコープの検証（位置から検索）
    // classPos は 5, 1, 15, 1（1ベース）なので、0ベースでは 4, 0, 14, 0
    // methodPos は 7, 5, 10, 5（1ベース）なので、0ベースでは 6, 4, 9, 4
    // 行5（0ベース）はクラススコープ内だがメソッドスコープ外
    Option<Scope> classScope = rootScope.findScopeAt(5, 0);
    assertTrue(classScope.isDefined(), "クラススコープが見つかりません。");
    assertEquals(Scope.ScopeType.CLASS, classScope.get().getType());

    // フィールドがクラススコープに追加されているか確認
    Option<SymbolDefinition> fieldDef = classScope.get().findLocalSymbol("testField");
    assertTrue(fieldDef.isDefined());

    // メソッドスコープの検証
    io.vavr.collection.List<Scope> classChildren = classScope.get().getChildren();
    assertEquals(1, classChildren.size());
    Scope methodScope = classChildren.get(0);
    assertEquals(Scope.ScopeType.METHOD, methodScope.getType());

    // パラメータがメソッドスコープに追加されているか確認
    Option<SymbolDefinition> param1 = methodScope.findLocalSymbol("param1");
    assertTrue(param1.isDefined());
    assertEquals(SymbolDefinition.DefinitionType.PARAMETER, param1.get().definitionType());

    Option<SymbolDefinition> param2 = methodScope.findLocalSymbol("param2");
    assertTrue(param2.isDefined());
  }

  @Test
  void testClearAndRebuild() {
    // Given
    String uri = "file:///test.groovy";
    var pos = new ClassInfo.Position(1, 1, 10, 1);
    var classInfo1 =
        new ClassInfo(
            "OldClass",
            "com.example.OldClass",
            ClassInfo.ClassType.CLASS,
            pos,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var classInfo2 =
        new ClassInfo(
            "NewClass",
            "com.example.NewClass",
            ClassInfo.ClassType.CLASS,
            pos,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            java.lang.reflect.Modifier.PUBLIC);

    var astInfo1 = new AstInfo(uri, List.of(classInfo1), List.of(), "com.example", List.of());
    var astInfo2 = new AstInfo(uri, List.of(classInfo2), List.of(), "com.example", List.of());

    // When
    service.buildSymbolTable(astInfo1, uri, symbolTable);
    service.buildSymbolTable(astInfo2, uri, symbolTable);

    // Then
    // 古いシンボルがクリアされているか確認
    Option<SymbolDefinition> oldClass = symbolTable.findByQualifiedName("com.example.OldClass");
    assertTrue(oldClass.isEmpty());

    // 新しいシンボルが追加されているか確認
    Option<SymbolDefinition> newClass = symbolTable.findByQualifiedName("com.example.NewClass");
    assertTrue(newClass.isDefined());
  }
}
