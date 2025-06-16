package com.groovylsp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.testing.SlowTest;
import io.vavr.collection.List;
import java.util.Random;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SymbolTableのパフォーマンステスト */
@SlowTest
class SymbolTablePerformanceTest {

  private SymbolTable symbolTable;
  private static final int LARGE_SYMBOL_COUNT = 10000;
  private static final int SEARCH_ITERATIONS = 1000;

  @BeforeEach
  void setUp() {
    symbolTable = new SymbolTable();
  }

  @Test
  @DisplayName("大規模なシンボルテーブルでの検索パフォーマンス")
  void testLargeScaleSearchPerformance() {
    // 大量のシンボルを追加
    for (int i = 0; i < LARGE_SYMBOL_COUNT; i++) {
      String name = "symbol" + (i % 100); // 100種類の名前を繰り返し使用
      String qualifiedName = "com.example.Class" + (i % 10) + "." + name;
      var definition =
          new SymbolDefinition(
              name,
              qualifiedName,
              SymbolKind.Variable,
              "file:///test" + (i % 50) + ".groovy",
              new Range(new Position(i, 0), new Position(i, 10)),
              new Range(new Position(i, 0), new Position(i, 10)),
              "com.example.Class" + (i % 10),
              SymbolDefinition.DefinitionType.FIELD);
      symbolTable.addSymbol(definition);
    }

    // 検索パフォーマンスを測定
    long startTime = System.currentTimeMillis();
    var random = new Random(42); // 再現性のためシード固定

    for (int i = 0; i < SEARCH_ITERATIONS; i++) {
      String searchName = "symbol" + random.nextInt(100);
      List<SymbolDefinition> results = symbolTable.findByName(searchName);
      assertNotNull(results);
      assertFalse(results.isEmpty());
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // 1000回の検索が1秒以内に完了することを確認
    assertTrue(duration < 1000, "検索が遅すぎます: " + duration + "ms (期待値: < 1000ms)");

    System.out.println(
        "大規模検索パフォーマンス: "
            + duration
            + "ms for "
            + SEARCH_ITERATIONS
            + " searches in "
            + LARGE_SYMBOL_COUNT
            + " symbols");
  }

  @Test
  @DisplayName("キャッシュの効果測定")
  void testCacheEffectiveness() {
    // シンボルを追加
    for (int i = 0; i < 1000; i++) {
      String name = "cached" + (i % 10); // 10種類の名前
      var definition =
          new SymbolDefinition(
              name,
              "com.example." + name + i,
              SymbolKind.Variable,
              "file:///test.groovy",
              new Range(new Position(i, 0), new Position(i, 10)),
              new Range(new Position(i, 0), new Position(i, 10)),
              "com.example.Test",
              SymbolDefinition.DefinitionType.LOCAL_VARIABLE);
      symbolTable.addSymbol(definition);
    }

    // 最初のアクセス（キャッシュなし）
    long firstAccessTime = measureAverageSearchTime("cached5", 100);

    // 2回目のアクセス（キャッシュあり）
    long cachedAccessTime = measureAverageSearchTime("cached5", 100);

    // キャッシュによる高速化を確認
    assertTrue(
        cachedAccessTime <= firstAccessTime,
        "キャッシュアクセスが初回アクセスより遅い: "
            + "first="
            + firstAccessTime
            + "ns, cached="
            + cachedAccessTime
            + "ns");

    System.out.println(
        "キャッシュ効果: first=" + firstAccessTime + "ns, cached=" + cachedAccessTime + "ns");
  }

  @Test
  @DisplayName("同名シンボルの優先順位解決パフォーマンス")
  void testSameNameSymbolPriorityPerformance() {
    String commonName = "commonSymbol";

    // 同じ名前で異なるファイル・スコープのシンボルを追加
    for (int i = 0; i < 100; i++) {
      for (int j = 0; j < 10; j++) {
        var definition =
            new SymbolDefinition(
                commonName,
                "com.example.Class" + i + "." + commonName,
                SymbolKind.Variable,
                "file:///test" + i + ".groovy",
                new Range(new Position(j * 10, 0), new Position(j * 10 + 5, 0)),
                new Range(new Position(j * 10, 0), new Position(j * 10 + 5, 0)),
                "com.example.Class" + i,
                SymbolDefinition.DefinitionType.FIELD);
        symbolTable.addSymbol(definition);
      }
    }

    // 同名シンボルの検索パフォーマンス
    long startTime = System.nanoTime();
    List<SymbolDefinition> results = symbolTable.findByName(commonName);
    long endTime = System.nanoTime();

    assertEquals(1000, results.size()); // 100 files * 10 symbols

    long duration = (endTime - startTime) / 1000; // マイクロ秒に変換
    assertTrue(
        duration < 10000, // 10msec以内
        "同名シンボルの検索が遅すぎます: " + duration + "μs");

    System.out.println("同名シンボル検索: " + results.size() + " symbols found in " + duration + "μs");
  }

  private long measureAverageSearchTime(String name, int iterations) {
    long totalTime = 0;
    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      symbolTable.findByName(name);
      long end = System.nanoTime();
      totalTime += (end - start);
    }
    return totalTime / iterations;
  }
}
