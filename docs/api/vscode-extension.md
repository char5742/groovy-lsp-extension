# VSCode Groovy拡張機能API仕様

## 概要

本ドキュメントは、Groovy VSCode拡張機能が提供するコマンド、設定、および拡張APIの仕様を定義します。

## 拡張機能情報

```json
{
    "name": "groovy-lsp",
    "displayName": "Groovy Language Support",
    "description": "Groovy and Spock support with LSP",
    "version": "1.0.0",
    "publisher": "groovy-lsp-team",
    "engines": {
        "vscode": "^1.75.0"
    },
    "categories": ["Programming Languages", "Linters", "Formatters"],
    "activationEvents": [
        "onLanguage:groovy",
        "workspaceContains:**/*.groovy",
        "workspaceContains:**/*.gradle"
    ]
}
```

## コマンド

### 基本コマンド

| コマンドID | タイトル | 説明 | キーバインド |
|-----------|---------|------|-------------|
| `groovy.restartServer` | Restart Groovy Language Server | LSPサーバーを再起動 | - |
| `groovy.showOutputChannel` | Show Groovy Output | 出力チャンネルを表示 | - |
| `groovy.clearCache` | Clear Groovy Cache | キャッシュをクリア | - |

### コード編集コマンド

| コマンドID | タイトル | 説明 | キーバインド |
|-----------|---------|------|-------------|
| `groovy.organizeImports` | Organize Imports | インポートを整理 | `Shift+Alt+O` |
| `groovy.generateGettersSetters` | Generate Getters/Setters | getter/setterを生成 | - |
| `groovy.generateConstructor` | Generate Constructor | コンストラクタを生成 | - |
| `groovy.generateToString` | Generate toString | toStringメソッドを生成 | - |
| `groovy.generateEquals` | Generate equals/hashCode | equals/hashCodeを生成 | - |

### テスト関連コマンド

| コマンドID | タイトル | 説明 | キーバインド |
|-----------|---------|------|-------------|
| `groovy.runTest` | Run Groovy Test | カーソル位置のテストを実行 | `Ctrl+Shift+T` |
| `groovy.runAllTests` | Run All Tests | すべてのテストを実行 | - |
| `groovy.debugTest` | Debug Groovy Test | テストをデバッグ実行 | `Ctrl+Shift+D` |
| `groovy.runTestFile` | Run Test File | ファイル内の全テストを実行 | - |

### Spock特有コマンド

| コマンドID | タイトル | 説明 | キーバインド |
|-----------|---------|------|-------------|
| `groovy.spock.generateTest` | Generate Spock Test | Spockテストを生成 | - |
| `groovy.spock.addDataRow` | Add Data Row | whereブロックに行を追加 | - |
| `groovy.spock.convertToParameterized` | Convert to Parameterized | パラメータ化テストに変換 | - |
| `groovy.spock.extractDataTable` | Extract Data Table | データテーブルを抽出 | - |

## 設定

### 基本設定

```typescript
{
    // Groovy SDK設定
    "groovy.sdk.home": {
        "type": "string",
        "default": null,
        "description": "Path to Groovy SDK home directory"
    },
    
    // クラスパス設定
    "groovy.classpath.includeProjectDependencies": {
        "type": "boolean",
        "default": true,
        "description": "Include project dependencies in classpath"
    },
    
    // 言語サーバー設定
    "groovy.lsp.enabled": {
        "type": "boolean",
        "default": true,
        "description": "Enable Groovy Language Server"
    },
    
    "groovy.lsp.maxMemory": {
        "type": "string",
        "default": "1G",
        "description": "Maximum heap size for language server"
    },
    
    "groovy.lsp.vmargs": {
        "type": "array",
        "default": [],
        "description": "Additional JVM arguments for language server"
    }
}
```

### エディタ設定

```typescript
{
    // 自動補完
    "groovy.completion.autoImport": {
        "type": "boolean",
        "default": true,
        "description": "Enable auto-import in code completion"
    },
    
    "groovy.completion.guessMethodArguments": {
        "type": "boolean",
        "default": true,
        "description": "Enable parameter hints in code completion"
    },
    
    "groovy.completion.showDeprecated": {
        "type": "boolean",
        "default": true,
        "description": "Show deprecated items in completion"
    },
    
    // フォーマット
    "groovy.format.enabled": {
        "type": "boolean",
        "default": true,
        "description": "Enable Groovy formatting"
    },
    
    "groovy.format.style": {
        "type": "string",
        "enum": ["default", "google", "custom"],
        "default": "default",
        "description": "Code formatting style"
    }
}
```

### Spock設定

```typescript
{
    "groovy.spock.enabled": {
        "type": "boolean",
        "default": true,
        "description": "Enable Spock framework support"
    },
    
    "groovy.spock.generateBDDStyle": {
        "type": "boolean",
        "default": true,
        "description": "Generate tests in BDD style"
    },
    
    "groovy.spock.dataTableAlignment": {
        "type": "boolean",
        "default": true,
        "description": "Auto-align data tables in where blocks"
    },
    
    "groovy.spock.showBlockHints": {
        "type": "boolean",
        "default": true,
        "description": "Show inline hints for Spock blocks"
    }
}
```

### 診断設定

```typescript
{
    "groovy.diagnostic.enabled": {
        "type": "boolean",
        "default": true,
        "description": "Enable diagnostics"
    },
    
    "groovy.diagnostic.typeCheck": {
        "type": "string",
        "enum": ["none", "static", "dynamic"],
        "default": "dynamic",
        "description": "Type checking level"
    },
    
    "groovy.diagnostic.deprecatedWarnings": {
        "type": "boolean",
        "default": true,
        "description": "Show warnings for deprecated APIs"
    }
}
```

## 拡張API

他の拡張機能から利用可能なAPIを提供します。

### API定義

```typescript
interface GroovyExtensionAPI {
    // バージョン情報
    readonly version: string;
    
    // LSPクライアント
    getLanguageClient(): Promise<LanguageClient>;
    
    // プロジェクト情報
    getProjectInfo(uri: Uri): Promise<ProjectInfo>;
    
    // テスト実行
    runTests(testFilter?: TestFilter): Promise<TestResult[]>;
    
    // コード生成
    generateCode(params: GenerateCodeParams): Promise<TextEdit[]>;
    
    // Spock機能
    spock: {
        isSpockFile(uri: Uri): Promise<boolean>;
        getTestMethods(uri: Uri): Promise<TestMethod[]>;
        generateDataTable(params: DataTableParams): Promise<string>;
    };
}
```

### 型定義

```typescript
interface ProjectInfo {
    name: string;
    groovyVersion: string;
    dependencies: Dependency[];
    sourceRoots: string[];
    testRoots: string[];
}

interface TestFilter {
    uri?: Uri;
    testName?: string;
    className?: string;
}

interface TestResult {
    name: string;
    className: string;
    status: "passed" | "failed" | "skipped";
    duration: number;
    error?: {
        message: string;
        stackTrace: string;
    };
}

interface GenerateCodeParams {
    uri: Uri;
    position: Position;
    type: "getter" | "setter" | "constructor" | "toString" | "equals";
    fields?: string[];
}

interface TestMethod {
    name: string;
    range: Range;
    type: "spock" | "junit";
    dataProviders?: string[];
}

interface DataTableParams {
    headers: string[];
    rows?: string[][];
    alignment?: "left" | "right" | "center";
}
```

### 使用例

```typescript
// 他の拡張機能から利用
const groovyExt = vscode.extensions.getExtension('groovy-lsp-team.groovy-lsp');
if (groovyExt) {
    const api = groovyExt.exports as GroovyExtensionAPI;
    
    // プロジェクト情報の取得
    const projectInfo = await api.getProjectInfo(document.uri);
    console.log(`Groovy version: ${projectInfo.groovyVersion}`);
    
    // テストの実行
    const results = await api.runTests({
        uri: document.uri,
        testName: 'shouldCalculateCorrectly'
    });
    
    // Spockファイルかチェック
    if (await api.spock.isSpockFile(document.uri)) {
        const methods = await api.spock.getTestMethods(document.uri);
        console.log(`Found ${methods.length} test methods`);
    }
}
```

## イベント

### カスタムイベント

```typescript
// テスト実行イベント
interface TestRunEvent {
    type: "started" | "finished";
    tests: TestInfo[];
}

// プロジェクト変更イベント
interface ProjectChangeEvent {
    type: "dependencies" | "classpath" | "groovyVersion";
    project: Uri;
}

// 使用例
api.onDidRunTests((event: TestRunEvent) => {
    if (event.type === 'finished') {
        showTestResults(event.tests);
    }
});
```

## コードレンズ

### 提供されるコードレンズ

```typescript
interface GroovyCodeLens extends CodeLens {
    // テスト実行
    command?: {
        title: "Run" | "Debug" | "Run All";
        command: "groovy.runTest" | "groovy.debugTest" | "groovy.runAllTests";
        arguments?: any[];
    };
    
    // 参照数
    data?: {
        type: "references";
        count: number;
    };
}
```

## デバッグ設定

### launch.json設定

```json
{
    "type": "groovy",
    "request": "launch",
    "name": "Run Groovy Script",
    "program": "${file}",
    "args": [],
    "cwd": "${workspaceFolder}",
    "console": "integratedTerminal",
    "internalConsoleOptions": "neverOpen"
}
```

### デバッグアダプター設定

```json
{
    "type": "groovy",
    "request": "attach",
    "name": "Attach to Groovy Process",
    "hostName": "localhost",
    "port": 5005,
    "timeout": 30000,
    "sourcePaths": ["${workspaceFolder}/src"]
}
```

## エラーコード

拡張機能固有のエラーコード：

| コード | 説明 |
|--------|------|
| `GROOVY_EXT_001` | Groovy SDKが見つかりません |
| `GROOVY_EXT_002` | LSPサーバーの起動に失敗 |
| `GROOVY_EXT_003` | プロジェクト設定の読み込みエラー |
| `GROOVY_EXT_004` | テスト実行エラー |
| `GROOVY_EXT_005` | デバッガー接続エラー |