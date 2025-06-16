package com.groovylsp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.testing.FastTest;
import io.vavr.collection.List;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 同名シンボルの優先順位解決に関するテスト */
@FastTest
class SymbolPriorityTest {

  private SymbolTable symbolTable;
  private ScopeManager scopeManager;

  @BeforeEach
  void setUp() {
    symbolTable = new SymbolTable();
    scopeManager = new ScopeManager();
  }

  @Test
  @DisplayName("同じファイル内の同名シンボルは位置で区別される")
  void testSameNameSymbolsInSameFile() {
    String uri = "file:///test.groovy";
    String symbolName = "duplicateName";

    // 異なる位置に同名のシンボルを定義
    var symbol1 =
        new SymbolDefinition(
            symbolName,
            "Class1." + symbolName,
            SymbolKind.Field,
            uri,
            new Range(new Position(10, 0), new Position(10, 20)),
            new Range(new Position(10, 0), new Position(10, 20)),
            "Class1",
            SymbolDefinition.DefinitionType.FIELD);

    var symbol2 =
        new SymbolDefinition(
            symbolName,
            "Class2." + symbolName,
            SymbolKind.Field,
            uri,
            new Range(new Position(30, 0), new Position(30, 20)),
            new Range(new Position(30, 0), new Position(30, 20)),
            "Class2",
            SymbolDefinition.DefinitionType.FIELD);

    symbolTable.addSymbol(symbol1);
    symbolTable.addSymbol(symbol2);

    // 同名シンボルが両方取得できることを確認
    List<SymbolDefinition> found = symbolTable.findByName(symbolName);
    assertEquals(2, found.size());
    assertTrue(found.contains(symbol1));
    assertTrue(found.contains(symbol2));
  }

  @Test
  @DisplayName("異なるファイルの同名シンボルの優先順位")
  void testSameNameSymbolsAcrossFiles() {
    String symbolName = "commonFunction";
    String currentFile = "file:///current.groovy";
    String otherFile = "file:///other.groovy";

    // 現在のファイルのシンボル
    var currentFileSymbol =
        new SymbolDefinition(
            symbolName,
            "CurrentClass." + symbolName,
            SymbolKind.Method,
            currentFile,
            new Range(new Position(5, 0), new Position(10, 0)),
            new Range(new Position(5, 0), new Position(5, 20)),
            "CurrentClass",
            SymbolDefinition.DefinitionType.METHOD);

    // 他のファイルのシンボル
    var otherFileSymbol =
        new SymbolDefinition(
            symbolName,
            "OtherClass." + symbolName,
            SymbolKind.Method,
            otherFile,
            new Range(new Position(5, 0), new Position(10, 0)),
            new Range(new Position(5, 0), new Position(5, 20)),
            "OtherClass",
            SymbolDefinition.DefinitionType.METHOD);

    symbolTable.addSymbol(currentFileSymbol);
    symbolTable.addSymbol(otherFileSymbol);

    // 名前で検索
    List<SymbolDefinition> allSymbols = symbolTable.findByName(symbolName);
    assertEquals(2, allSymbols.size());

    // 同じファイル内のシンボルをフィルタ
    List<SymbolDefinition> sameFileSymbols = allSymbols.filter(s -> s.uri().equals(currentFile));
    assertEquals(1, sameFileSymbols.size());
    assertEquals(currentFileSymbol, sameFileSymbols.head());
  }

  @Test
  @DisplayName("スコープとシンボルテーブルの統合的な優先順位解決")
  void testIntegratedPriorityResolution() {
    String uri = "file:///test.groovy";
    String symbolName = "value";

    // ファイル全体のスコープ
    var fileScope =
        new Scope(
            Scope.ScopeType.GLOBAL,
            null,
            new Range(new Position(0, 0), new Position(100, 0)),
            null);
    scopeManager.setRootScope(uri, fileScope);

    // クラススコープ
    var classScope =
        new Scope(
            Scope.ScopeType.CLASS,
            fileScope,
            new Range(new Position(10, 0), new Position(90, 0)),
            "TestClass");

    // メソッドスコープ
    var methodScope =
        new Scope(
            Scope.ScopeType.METHOD,
            classScope,
            new Range(new Position(20, 0), new Position(80, 0)),
            "testMethod");

    // 各レベルでシンボルを定義
    var globalSymbol =
        new SymbolDefinition(
            symbolName,
            "global." + symbolName,
            SymbolKind.Variable,
            uri,
            new Range(new Position(5, 0), new Position(5, 10)),
            new Range(new Position(5, 0), new Position(5, 10)),
            null,
            SymbolDefinition.DefinitionType.FIELD);

    var classSymbol =
        new SymbolDefinition(
            symbolName,
            "TestClass." + symbolName,
            SymbolKind.Field,
            uri,
            new Range(new Position(15, 0), new Position(15, 10)),
            new Range(new Position(15, 0), new Position(15, 10)),
            "TestClass",
            SymbolDefinition.DefinitionType.FIELD);

    var methodSymbol =
        new SymbolDefinition(
            symbolName,
            "TestClass.testMethod." + symbolName,
            SymbolKind.Variable,
            uri,
            new Range(new Position(25, 0), new Position(25, 10)),
            new Range(new Position(25, 0), new Position(25, 10)),
            "TestClass.testMethod",
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);

    // シンボルを追加
    fileScope.addSymbol(globalSymbol);
    classScope.addSymbol(classSymbol);
    methodScope.addSymbol(methodSymbol);
    symbolTable.addSymbol(globalSymbol);
    symbolTable.addSymbol(classSymbol);
    symbolTable.addSymbol(methodSymbol);

    // メソッド内の位置からシンボルを検索
    var foundInMethod = scopeManager.findSymbolAt(uri, new Position(30, 0), symbolName);
    assertTrue(foundInMethod.isDefined());
    assertEquals(methodSymbol, foundInMethod.get()); // 最も近いスコープのシンボルが優先

    // クラス内（メソッド外）の位置から検索
    var foundInClass = scopeManager.findSymbolAt(uri, new Position(85, 0), symbolName);
    assertTrue(foundInClass.isDefined());
    assertEquals(classSymbol, foundInClass.get());

    // グローバルスコープから検索
    var foundGlobal = scopeManager.findSymbolAt(uri, new Position(95, 0), symbolName);
    assertTrue(foundGlobal.isDefined());
    assertEquals(globalSymbol, foundGlobal.get());
  }
}
