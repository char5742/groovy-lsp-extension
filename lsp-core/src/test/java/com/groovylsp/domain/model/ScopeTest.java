package com.groovylsp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.testing.FastTest;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.Test;

/** Scopeの単体テスト */
@FastTest
class ScopeTest {

  @Test
  void testAddAndFindSymbol() {
    // Given
    var scopeRange = new Range(new Position(0, 0), new Position(50, 0));
    var scope = new Scope(Scope.ScopeType.GLOBAL, null, scopeRange, null);

    String uri = "file:///test.groovy";
    var symbolRange = new Range(new Position(5, 0), new Position(10, 0));
    var symbol =
        new SymbolDefinition(
            "testVar",
            "testVar",
            SymbolKind.Variable,
            uri,
            symbolRange,
            symbolRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);

    // When
    scope.addSymbol(symbol);

    // Then
    Option<SymbolDefinition> found = scope.findSymbol("testVar");
    assertTrue(found.isDefined());
    assertEquals(symbol, found.get());

    Option<SymbolDefinition> notFound = scope.findSymbol("notExists");
    assertTrue(notFound.isEmpty());
  }

  @Test
  void testParentScopeSymbolResolution() {
    // Given
    var parentRange = new Range(new Position(0, 0), new Position(100, 0));
    var parentScope = new Scope(Scope.ScopeType.GLOBAL, null, parentRange, null);

    var childRange = new Range(new Position(20, 0), new Position(50, 0));
    var childScope = new Scope(Scope.ScopeType.METHOD, parentScope, childRange, "testMethod");

    String uri = "file:///test.groovy";
    var parentSymbol =
        new SymbolDefinition(
            "parentVar",
            "parentVar",
            SymbolKind.Variable,
            uri,
            parentRange,
            parentRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
    var childSymbol =
        new SymbolDefinition(
            "childVar",
            "childVar",
            SymbolKind.Variable,
            uri,
            childRange,
            childRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);

    // When
    parentScope.addSymbol(parentSymbol);
    childScope.addSymbol(childSymbol);

    // Then
    // 子スコープから親スコープのシンボルを参照できる
    Option<SymbolDefinition> foundParent = childScope.findSymbol("parentVar");
    assertTrue(foundParent.isDefined());
    assertEquals(parentSymbol, foundParent.get());

    // 親スコープから子スコープのシンボルは参照できない
    Option<SymbolDefinition> notFoundChild = parentScope.findSymbol("childVar");
    assertTrue(notFoundChild.isEmpty());

    // 子スコープから自身のシンボルも参照できる
    Option<SymbolDefinition> foundChild = childScope.findSymbol("childVar");
    assertTrue(foundChild.isDefined());
    assertEquals(childSymbol, foundChild.get());
  }

  @Test
  void testFindScopeAt() {
    // Given
    var globalRange = new Range(new Position(0, 0), new Position(100, 0));
    var globalScope = new Scope(Scope.ScopeType.GLOBAL, null, globalRange, null);

    var classRange = new Range(new Position(10, 0), new Position(80, 0));
    var classScope = new Scope(Scope.ScopeType.CLASS, globalScope, classRange, "TestClass");

    var methodRange = new Range(new Position(20, 4), new Position(30, 4));
    var methodScope = new Scope(Scope.ScopeType.METHOD, classScope, methodRange, "testMethod");

    // When & Then
    // グローバルスコープ内の位置
    Option<Scope> foundGlobal = globalScope.findScopeAt(5, 0);
    assertTrue(foundGlobal.isDefined());
    assertEquals(globalScope, foundGlobal.get());

    // クラススコープ内の位置
    Option<Scope> foundClass = globalScope.findScopeAt(15, 0);
    assertTrue(foundClass.isDefined());
    assertEquals(classScope, foundClass.get());

    // メソッドスコープ内の位置
    Option<Scope> foundMethod = globalScope.findScopeAt(25, 6);
    assertTrue(foundMethod.isDefined());
    assertEquals(methodScope, foundMethod.get());

    // 範囲外の位置
    Option<Scope> notFound = globalScope.findScopeAt(101, 0);
    assertTrue(notFound.isEmpty());
  }

  @Test
  void testGetAllAvailableSymbols() {
    // Given
    var parentRange = new Range(new Position(0, 0), new Position(100, 0));
    var parentScope = new Scope(Scope.ScopeType.GLOBAL, null, parentRange, null);

    var childRange = new Range(new Position(20, 0), new Position(50, 0));
    var childScope = new Scope(Scope.ScopeType.METHOD, parentScope, childRange, "testMethod");

    String uri = "file:///test.groovy";
    var symbol1 =
        new SymbolDefinition(
            "var1",
            "var1",
            SymbolKind.Variable,
            uri,
            parentRange,
            parentRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
    var symbol2 =
        new SymbolDefinition(
            "var2",
            "var2",
            SymbolKind.Variable,
            uri,
            parentRange,
            parentRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
    var symbol3 =
        new SymbolDefinition(
            "var3",
            "var3",
            SymbolKind.Variable,
            uri,
            childRange,
            childRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);

    parentScope.addSymbol(symbol1);
    parentScope.addSymbol(symbol2);
    childScope.addSymbol(symbol3);

    // When
    List<SymbolDefinition> parentSymbols = parentScope.getAllAvailableSymbols();
    List<SymbolDefinition> childSymbols = childScope.getAllAvailableSymbols();

    // Then
    assertEquals(2, parentSymbols.size());
    assertTrue(parentSymbols.contains(symbol1));
    assertTrue(parentSymbols.contains(symbol2));

    assertEquals(3, childSymbols.size());
    assertTrue(childSymbols.contains(symbol1));
    assertTrue(childSymbols.contains(symbol2));
    assertTrue(childSymbols.contains(symbol3));
  }

  @Test
  void testShadowing() {
    // Given
    var parentRange = new Range(new Position(0, 0), new Position(100, 0));
    var parentScope = new Scope(Scope.ScopeType.GLOBAL, null, parentRange, null);

    var childRange = new Range(new Position(20, 0), new Position(50, 0));
    var childScope = new Scope(Scope.ScopeType.METHOD, parentScope, childRange, "testMethod");

    String uri = "file:///test.groovy";
    var parentSymbol =
        new SymbolDefinition(
            "shadowedVar",
            "parentScope.shadowedVar",
            SymbolKind.Variable,
            uri,
            parentRange,
            parentRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
    var childSymbol =
        new SymbolDefinition(
            "shadowedVar",
            "childScope.shadowedVar",
            SymbolKind.Variable,
            uri,
            childRange,
            childRange,
            null,
            SymbolDefinition.DefinitionType.LOCAL_VARIABLE);

    // When
    parentScope.addSymbol(parentSymbol);
    childScope.addSymbol(childSymbol);

    // Then
    // 子スコープでは子スコープのシンボルが優先される（シャドーイング）
    Option<SymbolDefinition> foundInChild = childScope.findSymbol("shadowedVar");
    assertTrue(foundInChild.isDefined());
    assertEquals(childSymbol, foundInChild.get());

    // 親スコープでは親スコープのシンボルが返される
    Option<SymbolDefinition> foundInParent = parentScope.findSymbol("shadowedVar");
    assertTrue(foundInParent.isDefined());
    assertEquals(parentSymbol, foundInParent.get());
  }
}
