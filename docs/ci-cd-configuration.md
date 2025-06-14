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
        run: cd groovy-lsp && ./gradlew errorProne
      
      - name: Run Spotless Check
        run: cd groovy-lsp && ./gradlew spotlessCheck
      
      - name: Run ArchUnit
        run: cd groovy-lsp && ./gradlew archUnit
      
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
        run: cd groovy-lsp && ./gradlew test
      
      - name: Generate JaCoCo Report
        run: cd groovy-lsp && ./gradlew jacocoTestReport
      
      - name: Check Java Coverage
        run: cd groovy-lsp && ./gradlew jacocoTestCoverageVerification
      
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
          cd groovy-lsp && ./gradlew build
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
            groovy-lsp/build/libs/*.jar
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

`groovy-lsp/build.gradle`:
```gradle
dependencies {
    // JSpecifyアノテーション
    api "org.jspecify:jspecify:1.0.0"
    
    // Error ProneとNullAway
    errorprone "com.google.errorprone:error_prone_core:2.18.0"
    errorprone "com.uber.nullaway:nullaway:0.10.24"
}

tasks.withType(JavaCompile).configureEach {
    options.errorprone {
        nullaway {
            annotatedPackages.add("com.groovylsp")
            treatGeneratedAsUnannotated = true
            // JSpecifyモード：@NullMarkedスコープのみチェック
            onlyNullMarked = true
            // 将来的にJSpecifyの完全サポートを有効化する場合
            // jspecifyMode = true
        }
    }
}
```

### Spotless

`groovy-lsp/build.gradle`:
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

`groovy-lsp/src/test/java/com/groovylsp/ArchitectureTest.java`:
```java
@AnalyzeClasses(packages = "com.groovylsp")
class ArchitectureTest {
    @ArchTest
    static final ArchRule onionArchitecture = 
        onionArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.service..")
            .applicationServices("..application..")
            .adapter("infrastructure", "..infrastructure..")
            .adapter("presentation", "..presentation..");
    
    @ArchTest
    static final ArchRule noCyclicDependencies = 
        slices().matching("com.groovylsp.(*)..")
            .should().beFreeOfCycles();
}
```

## カバレッジ設定

### JaCoCo設定

`groovy-lsp/build.gradle`:
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

## ローカル開発環境

### Lefthook設定

`.lefthook.yml`:
```yaml
pre-commit:
  parallel: true
  commands:
    java-format:
      glob: "*.java"
      run: cd groovy-lsp && ./gradlew spotlessApply
    
    typescript-lint:
      glob: "*.{ts,tsx}"
      run: cd vscode-extension && npm run lint:fix
    
    check-todos:
      run: grep -r "TODO\|FIXME" --include="*.java" --include="*.ts" . || true

pre-push:
  commands:
    test:
      run: |
        cd groovy-lsp && ./gradlew test
        cd ../vscode-extension && npm test
    
    coverage-check:
      run: |
        cd groovy-lsp && ./gradlew jacocoTestCoverageVerification
        cd ../vscode-extension && npm run coverage
```

### Git設定

`.gitconfig`:
```ini
[alias]
    # コミット時の--no-verify禁止
    commit = "!f() { \
        for arg in \"$@\"; do \
            case \"$arg\" in \
                --no-verify|-n) \
                    echo '🚫 --no-verify is prohibited'; \
                    exit 1;; \
            esac; \
        done; \
        command git commit \"$@\"; \
    }; f"
    
    # プッシュ時の--no-verify禁止
    push = "!f() { \
        for arg in \"$@\"; do \
            if [ \"$arg\" = \"--no-verify\" ]; then \
                echo '🚫 --no-verify is prohibited'; \
                exit 1; \
            fi; \
        done; \
        command git push \"$@\"; \
    }; f"
```

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
cd groovy-lsp && ./gradlew test --info
```

### 静的解析エラー

Error Proneのエラーを一時的に抑制:
```java
@SuppressWarnings("NullAway")
public void legacyMethod() {
    // TODO: リファクタリング予定
}
```