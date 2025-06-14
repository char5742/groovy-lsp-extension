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

カスタムアノテーションを作成してテストを分類します。高速テスト（100ms未満）、低速テスト（100ms以上）、外部リソース依存テスト、統合テストの四つのカテゴリでテストを管理します。

使用例：

カスタムアノテーションを使用して、テストの性質に応じて適切に分類します。ピュアな単体テストは@FastTest、統合テストは@IntegrationTest、計算量の多いテストは@SlowTestでマークします。

## 実行時間の可視化

### gradle-test-logger-pluginの設定

テスト実行時の出力を美しく表示するプラグインを設定します。mochaテーマを使用し、100ms以上のテストを遅いテストとして強調表示します。

### テスト実行時間レポート

カスタムタスクを定義してテスト実行時間をCSV形式で出力します。これによりパフォーマンスのボトルネックを特定し、最適化の対象を明確にします。

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

テストの性質に応じて適切なアノテーションを使用します。Fast Testはインメモリのデータのみを使用する純粋な単体テスト、Integration Testはファイルシステムやパーサーなどの外部リソースを使用する統合テストです。

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