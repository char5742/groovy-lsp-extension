# CI/CD設定ガイド

## 概要

本プロジェクトでは、GitHub Actionsを使用してCI/CDパイプラインを構築します。品質ゲートとして静的解析、テスト、カバレッジチェックを実施し、100%のテストカバレッジを維持します。

## パイプライン構成

### 1. プルリクエスト時（ci.yml）

```yaml
name: CI

on:
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  static-analysis:
    name: Static Analysis
    runs-on: ubuntu-latest
    steps:
      # Java静的解析
      - name: Run Error Prone
        run: cd lsp-core && ./gradlew errorProne
      
      - name: Run Spotless Check
        run: cd lsp-core && ./gradlew spotlessCheck
      
      - name: Run ArchUnit
        run: cd lsp-core && ./gradlew archUnit
      
      # TypeScript静的解析
      - name: Run ESLint
        run: cd vscode-extension && npm run lint
      
      - name: Run TypeScript Compiler
        run: cd vscode-extension && npm run compile -- --noEmit

  test:
    name: Test & Coverage
    runs-on: ubuntu-latest
    steps:
      # Javaテスト
      - name: Run Java Tests
        run: cd lsp-core && ./gradlew test
      
      - name: Generate JaCoCo Report
        run: cd lsp-core && ./gradlew jacocoTestReport
      
      - name: Check Java Coverage
        run: cd lsp-core && ./gradlew jacocoTestCoverageVerification
      
      # TypeScriptテスト
      - name: Run TypeScript Tests
        run: cd vscode-extension && npm test
      
      - name: Generate C8 Report
        run: cd vscode-extension && npm run coverage
      
      # OctoCoVで可視化
      - name: Run OctoCov
        uses: k1LoW/octocov-action@v0
```

### 2. mainブランチマージ時（deploy.yml）

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  integration-test:
    name: Integration Tests
    runs-on: ubuntu-latest
    steps:
      - name: Build All
        run: |
          cd lsp-core && ./gradlew build
          cd ../vscode-extension && npm run build
      
      - name: Run Integration Tests
        run: ./scripts/integration-test.sh

  release:
    name: Create Release
    needs: integration-test
    if: startsWith(github.ref, 'refs/tags/')
    runs-on: ubuntu-latest
    steps:
      - name: Build Release Artifacts
        run: ./scripts/build-release.sh
      
      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            lsp-core/build/libs/*.jar
            vscode-extension/*.vsix
```

### 3. TODO管理（todo-to-issue.yml）

```yaml
name: TODO to Issue

on:
  push:
    branches: [main, develop]

jobs:
  todo-to-issue:
    runs-on: ubuntu-latest
    steps:
      - uses: alstr/todo-to-issue-action@v4
        with:
          CLOSE_ISSUES: true
          AUTO_P: true
```

## 静的解析設定

### Error Prone + NullAway

Error ProneとNullAwayを使用して、コンパイル時のnull安全性チェックを実施します。JSpecifyアノテーションと連携し、@NullMarkedスコープのみをチェック対象とします。

### Spotless

`lsp-core/build.gradle`:
```gradle
spotless {
    java {
        googleJavaFormat('1.17.0')
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

### ArchUnit

ArchUnitを使用してアーキテクチャの制約を自動検証します。オニオンアーキテクチャのレイヤー間依存関係や、循環依存の不在をテストで保証します。

## カバレッジ設定

### JaCoCo設定

`lsp-core/build.gradle`:
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

`vscode-extension/package.json`:
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
    - lsp-core/build/reports/jacoco/test/jacocoTestReport.xml
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

## ローカル開発環境

### Lefthook設定

`.lefthook.yml`:
```yaml
pre-commit:
  parallel: true
  commands:
    java-format:
      glob: "*.java"
      run: cd lsp-core && ./gradlew spotlessApply
    
    typescript-lint:
      glob: "*.{ts,tsx}"
      run: cd vscode-extension && npm run lint:fix
    
    check-todos:
      run: grep -r "TODO\|FIXME" --include="*.java" --include="*.ts" . || true

pre-push:
  commands:
    test:
      run: |
        cd lsp-core && ./gradlew test
        cd ../vscode-extension && npm test
    
    coverage-check:
      run: |
        cd lsp-core && ./gradlew jacocoTestCoverageVerification
        cd ../vscode-extension && npm run coverage
```

### Git設定

Gitのエイリアスを設定して、--no-verifyオプションの使用を禁止します。これにより、フックのバイパスを防ぎ、品質チェックが必ず実行されるようにします。

## デバッグ設定

### GitHub Actions のデバッグ

ワークフローファイルに以下を追加してSSHデバッグ:
```yaml
- name: Setup tmate session
  uses: mxschmitt/action-tmate@v3
  if: ${{ failure() }}
```

### ローカルでのActions実行

```bash
# actを使用してローカルで実行
act -j static-analysis
act -j test
```

## トラブルシューティング

### カバレッジが100%にならない

1. 除外設定を確認
```gradle
jacocoTestReport {
    classDirectories.setFrom(files(classDirectories.files.collect {
        fileTree(dir: it, exclude: [
            '**/generated/**',
            '**/config/**'
        ])
    }))
}
```

2. テスト実行を確認
```bash
cd lsp-core && ./gradlew test --info
```

### 静的解析エラー

Error Proneのエラーを一時的に抑制:
```java
@SuppressWarnings("NullAway")
public void legacyMethod() {
    // TODO: リファクタリング予定
}
```