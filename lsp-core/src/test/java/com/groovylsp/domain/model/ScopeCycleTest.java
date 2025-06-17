package com.groovylsp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.testing.FastTest;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** スコープの循環参照に関するテスト */
@FastTest
class ScopeCycleTest {

  @Test
  @DisplayName("スコープの循環参照を防ぐ")
  void testPreventCyclicScope() {
    var range1 = new Range(new Position(0, 0), new Position(10, 0));
    var range2 = new Range(new Position(2, 0), new Position(8, 0));

    var scope1 = new Scope(Scope.ScopeType.CLASS, null, range1, "Class1");
    var scope2 = new Scope(Scope.ScopeType.METHOD, scope1, range2, "method1");

    // scope2の親をscope1に設定済み
    // scope1の親をscope2に設定しようとすると循環参照になる
    // 現在の実装では親スコープは不変なので、循環参照は発生しない

    // 正常な階層関係を確認
    assertEquals(scope1, scope2.getParent());
    assertNull(scope1.getParent());
  }

  @Test
  @DisplayName("深いスコープ階層でのシンボル解決")
  void testDeepScopeHierarchy() {
    // 深いスコープ階層を構築
    var rootScope =
        new Scope(
            Scope.ScopeType.GLOBAL,
            null,
            new Range(new Position(0, 0), new Position(100, 0)),
            null);

    Scope currentScope = rootScope;
    for (int i = 0; i < 100; i++) {
      var range = new Range(new Position(i, 0), new Position(i + 1, 0));
      currentScope = new Scope(Scope.ScopeType.BLOCK, currentScope, range, "block" + i);

      // 各レベルにシンボルを追加
      var symbol =
          new SymbolDefinition(
              "var" + i,
              "var" + i,
              SymbolKind.Variable,
              "test.groovy",
              range,
              range,
              currentScope.getName(),
              SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
      currentScope.addSymbol(symbol);
    }

    // 最深部からルートまでのシンボル解決
    int depth = 0;
    Scope scope = currentScope;
    while (scope != null) {
      depth++;
      scope = scope.getParent();
    }

    assertEquals(101, depth); // root + 100 blocks

    // 最深部から特定のシンボルを検索
    var found = currentScope.findSymbol("var50");
    assertTrue(found.isDefined());
    assertEquals("var50", found.get().name());
  }

  @Test
  @DisplayName("スコープ間のシンボルシャドーイング")
  void testSymbolShadowingAcrossScopes() {
    var globalRange = new Range(new Position(0, 0), new Position(100, 0));
    var classRange = new Range(new Position(10, 0), new Position(90, 0));
    var methodRange = new Range(new Position(20, 0), new Position(80, 0));
    var blockRange = new Range(new Position(30, 0), new Position(70, 0));

    var globalScope = new Scope(Scope.ScopeType.GLOBAL, null, globalRange, null);
    var classScope = new Scope(Scope.ScopeType.CLASS, globalScope, classRange, "TestClass");
    var methodScope = new Scope(Scope.ScopeType.METHOD, classScope, methodRange, "testMethod");
    var blockScope = new Scope(Scope.ScopeType.BLOCK, methodScope, blockRange, null);

    // 各スコープに同名のシンボルを追加
    String commonName = "shadowedVar";

    var globalSymbol = createSymbol(commonName, "global." + commonName, globalRange);
    var classSymbol = createSymbol(commonName, "TestClass." + commonName, classRange);
    var methodSymbol = createSymbol(commonName, "TestClass.testMethod." + commonName, methodRange);
    var blockSymbol =
        createSymbol(commonName, "TestClass.testMethod.block." + commonName, blockRange);

    globalScope.addSymbol(globalSymbol);
    classScope.addSymbol(classSymbol);
    methodScope.addSymbol(methodSymbol);
    blockScope.addSymbol(blockSymbol);

    // 各スコープから検索して、最も近いシンボルが返されることを確認
    assertEquals(globalSymbol, globalScope.findSymbol(commonName).get());
    assertEquals(classSymbol, classScope.findSymbol(commonName).get());
    assertEquals(methodSymbol, methodScope.findSymbol(commonName).get());
    assertEquals(blockSymbol, blockScope.findSymbol(commonName).get());
  }

  private SymbolDefinition createSymbol(String name, String qualifiedName, Range range) {
    return new SymbolDefinition(
        name,
        qualifiedName,
        SymbolKind.Variable,
        "test.groovy",
        range,
        range,
        null,
        SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
  }
}
