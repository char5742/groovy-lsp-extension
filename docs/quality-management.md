# 品質管理ガイド

## 概要

本プロジェクトでは、Googleが提唱するTest Pyramidの原則に基づいた品質管理を徹底します。実行時間の可視化とテストの適切な分類により、高速なフィードバックループを維持します。

## Test Pyramid戦略

### テストの層構造

```
        △ E2E Tests (10%)
       ／ ＼  実行時間: < 10s
      ／───＼ VSCode Extension統合テスト
     ／─────＼
    ／─────────＼ Integration Tests (20%)
   ／───────────＼ 実行時間: < 1s
  ／─────────────＼ モジュール間結合テスト
 ／───────────────＼
／─────────────────＼ Unit Tests (70%)
━━━━━━━━━━━━━━━━━━━ 実行時間: < 100ms
                     純粋な関数・クラステスト
```

### JUnitタグ設計

カスタムアノテーションを作成してテストを分類します：

```java
// テスト分類用のカスタムアノテーション
package com.groovylsp.test.annotations;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 高速テスト（100ms未満）
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("fast")
public @interface FastTest {
}

// 低速テスト（100ms以上）
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("slow")
public @interface SlowTest {
}

// 外部リソース依存テスト
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("external")
public @interface ExternalTest {
}

// 統合テスト（slow + external）
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("slow")
@Tag("external")
public @interface IntegrationTest {
}
```

使用例：

```java
import com.groovylsp.test.annotations.*;

class CompletionServiceTest {
    
    @FastTest
    @DisplayName("シンボル解決が正しく動作すること")
    void shouldResolveSymbol() {
        // ピュアな単体テスト
    }
    
    @IntegrationTest
    @DisplayName("ファイルシステムからの読み込みが正しく動作すること")
    void shouldReadFromFileSystem() {
        // 統合テスト
    }
    
    @SlowTest
    @DisplayName("大規模なASTの解析が正しく動作すること")
    void shouldParseLargeAst() {
        // 計算量の多いテスト
    }
}
```

## 実行時間の可視化

### gradle-test-logger-pluginの設定

`groovy-lsp/build.gradle`:
```gradle
plugins {
    id 'com.adarshr.test-logger' version '3.2.0'
}

testlogger {
    theme 'mocha'
    showExceptions true
    showStackTraces true
    showFullStackTraces false
    showCauses true
    slowThreshold 100 // 100ms以上を遅いテストとして強調
    showSummary true
    showSimpleNames false
    showPassed true
    showSkipped true
    showFailed true
    showStandardStreams false
    showPassedStandardStreams false
    showSkippedStandardStreams false
    showFailedStandardStreams false
}
```

### テスト実行時間レポート

```gradle
// カスタムタスクでテスト実行時間を集計
task testTimeReport {
    doLast {
        def reportFile = file("${buildDir}/reports/test-times.csv")
        reportFile.text = "Test Class,Method,Duration(ms),Tag\n"
        
        fileTree("${buildDir}/test-results/test").include("*.xml").each { file ->
            def xml = new XmlSlurper().parse(file)
            xml.testcase.each { testcase ->
                def duration = (testcase.@time.toDouble() * 1000).round()
                def tags = testcase.properties.property
                    .findAll { it.@name == 'tag' }
                    .collect { it.@value }
                    .join(';')
                reportFile << "${testcase.@classname},${testcase.@name},${duration},${tags}\n"
            }
        }
    }
}

test.finalizedBy testTimeReport
```

## テストの分類と実行

### Gradleタスクの定義

```gradle
// 高速テストのみ実行（CI用）
task fastTest(type: Test) {
    useJUnitPlatform {
        includeTags 'fast'
    }
    failFast = true
}

// 統合テストを含む実行
task integrationTest(type: Test) {
    useJUnitPlatform {
        includeTags 'slow', 'external'
    }
}

// 全テスト実行
test {
    useJUnitPlatform()
    
    // 並列実行の設定
    maxParallelForks = Runtime.runtime.availableProcessors()
    
    // ヒープサイズの設定
    minHeapSize = "512m"
    maxHeapSize = "2048m"
}
```

### CI/CDでの使い分け

```yaml
# .github/workflows/ci.yml
jobs:
  fast-feedback:
    name: Fast Tests
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Run Fast Tests
        run: cd groovy-lsp && ./gradlew fastTest
  
  full-test:
    name: Full Test Suite
    needs: fast-feedback
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - name: Run All Tests
        run: cd groovy-lsp && ./gradlew test
```

## CodeQL統合

GitHubのデフォルトCodeQL設定を使用します。リポジトリのSecurity設定から有効化してください。

## カバレッジ設定

### JaCoCo設定

```gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.8  // 80%カバレッジ
            }
        }
    }
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
```

### C8設定

```json
{
  "scripts": {
    "coverage": "c8 --check-coverage --lines 80 --functions 80 --branches 80 npm test"
  },
  "c8": {
    "include": ["src/**/*.ts"],
    "exclude": ["**/*.test.ts"],
    "reporter": ["text", "lcov", "html"]
  }
}
```

### OctoCov設定

`.octocov.yml`:
```yaml
coverage:
  paths:
    - groovy-lsp/build/reports/jacoco/test/jacocoTestReport.xml
    - vscode-extension/coverage/lcov.info
  
  badge:
    path: docs/coverage.svg
  
  threshold:
    total: 80
    
comment:
  enable: true
  
report:
  enable: true
  path: docs/coverage-report.md
```

## テスト品質メトリクス

### ダッシュボード設定

```gradle
// テスト品質レポートの生成
task testQualityReport {
    dependsOn test, jacocoTestReport
    
    doLast {
        def metrics = [:]
        
        // テストカウント
        def testResults = file("${buildDir}/test-results/test")
        def fastTests = 0
        def slowTests = 0
        def externalTests = 0
        
        // カバレッジ情報
        def coverageReport = file("${buildDir}/reports/jacoco/test/jacocoTestReport.xml")
        
        // メトリクスの集計と出力
        def reportFile = file("${buildDir}/reports/test-quality-metrics.json")
        reportFile.text = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(metrics))
    }
}
```

### 品質ゲート

```gradle
task checkTestQuality {
    dependsOn testQualityReport
    
    doLast {
        def metrics = new groovy.json.JsonSlurper().parse(file("${buildDir}/reports/test-quality-metrics.json"))
        
        // テストピラミッドの比率チェック
        def unitRatio = metrics.fastTests / metrics.totalTests
        def integrationRatio = metrics.slowTests / metrics.totalTests
        
        if (unitRatio < 0.6) {
            throw new GradleException("Unit test ratio is too low: ${unitRatio * 100}% (expected >= 60%)")
        }
        
        if (integrationRatio > 0.3) {
            throw new GradleException("Integration test ratio is too high: ${integrationRatio * 100}% (expected <= 30%)")
        }
    }
}
```

## ベストプラクティス

### 1. テストの書き方

```java
import com.groovylsp.test.annotations.*;

// Fast Test - 純粋な単体テスト
@FastTest
void shouldCalculateCompletionScore() {
    // Given - インメモリのデータのみ使用
    var symbol = Symbol.of("testMethod", SymbolKind.Method);
    var context = CompletionContext.of("test", 4);
    
    // When - 純粋関数の呼び出し
    var score = scorer.calculate(symbol, context);
    
    // Then
    assertThat(score).isEqualTo(0.8);
}

// Integration Test - 統合テスト
@IntegrationTest
void shouldCompleteFromWorkspace() {
    // Given - ファイルシステムやパーサーを使用
    var workspace = TestWorkspace.create();
    workspace.addFile("Test.groovy", "class Test { }");
    
    // When
    var completions = service.complete(workspace, position);
    
    // Then
    assertThat(completions).isNotEmpty();
}
```

### 2. テスト時間の監視

- PR時に実行時間が増加した場合、コメントで警告
- 週次でテスト実行時間のトレンドレポート生成
- 遅いテストのリファクタリングを定期的に実施

### 3. 並列実行の活用

```gradle
test {
    // CPU数に応じた並列実行
    maxParallelForks = Runtime.runtime.availableProcessors()
    
    // クラス単位での並列実行
    forkEvery = 1
}
```