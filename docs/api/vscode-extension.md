# VSCode Groovy拡張機能API仕様

## 概要

本ドキュメントは、Groovy VSCode拡張機能が提供するコマンド、設定、および拡張APIの仕様を定義します。

## 拡張機能情報

```json
{
    "name": "lsp-core",
    "displayName": "Groovy Language Support",
    "description": "Groovy and Spock support with LSP",
    "version": "1.0.0",
    "publisher": "lsp-core-team",
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
    
    "groovy.spock.dataTableAlignment": {
        "type": "boolean",
        "default": true,
        "description": "Auto-align data tables in where blocks"
    },
    
    "groovy.spock.showBlockHints": {
        "type": "boolean",
        "default": true,
        "description": "Show inline hints for Spock blocks"
    },
    
    "groovy.spock.mockGeneration": {
        "type": "string",
        "enum": ["auto", "manual"],
        "default": "auto",
        "description": "Mock generation style"
    },
    
    "groovy.spock.interactionValidation": {
        "type": "boolean",
        "default": true,
        "description": "Validate mock interactions"
    },
    
    "groovy.spock.assertionVisualization": {
        "type": "boolean",
        "default": true,
        "description": "Show power assertion visualization on hover"
    },
    
    "groovy.spock.lifecycleMethodGeneration": {
        "type": "boolean",
        "default": true,
        "description": "Enable lifecycle method generation"
    },
    
    "groovy.spock.dataPipeCompletion": {
        "type": "boolean",
        "default": true,
        "description": "Enable completion for data pipe syntax"
    },
    
    "groovy.spock.configFile": {
        "type": "string",
        "default": "SpockConfig.groovy",
        "description": "Path to Spock configuration file"
    },
    
    "groovy.spock.parallelExecution": {
        "type": "boolean",
        "default": false,
        "description": "Enable parallel test execution"
    },
    
    "groovy.spock.parallelExecutionMode": {
        "type": "string",
        "enum": ["fixed", "dynamic"],
        "default": "dynamic",
        "description": "Parallel execution mode"
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
    // Spock機能
    spock: {
        isSpockFile(uri: Uri): Promise<boolean>;
        getTestMethods(uri: Uri): Promise<TestMethod[]>;
        getBlockStructure(uri: Uri): Promise<SpockBlockInfo[]>;
        validateInteractions(uri: Uri): Promise<InteractionValidation[]>;
        getSharedFields(uri: Uri): Promise<SharedFieldInfo[]>;
        getLifecycleMethods(uri: Uri): Promise<LifecycleMethod[]>;
        getDataProviders(uri: Uri): Promise<DataProvider[]>;
        getAssertionInfo(uri: Uri, position: Position): Promise<AssertionInfo>;
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

interface MockGenerationParams {
    uri: Uri;
    position: Position;
    type: "mock" | "stub" | "spy";
    className: string;
    variableName?: string;
    options?: {
        global?: boolean;
        verified?: boolean;
        defaultResponse?: any;
    };
}

interface InteractionParams {
    mockName: string;
    method: string;
    arguments?: string[];
    cardinality?: string; // e.g., "1", "1..3", "_"
    response?: string;
}

interface SpockBlockInfo {
    type: "given" | "when" | "then" | "expect" | "where" | "and" | "cleanup";
    range: Range;
    label?: string;
    hasInteractions?: boolean;
}

interface InteractionValidation {
    range: Range;
    isValid: boolean;
    message?: string;
    severity: "error" | "warning";
}

interface SharedFieldInfo {
    name: string;
    type: string;
    range: Range;
    isInitialized: boolean;
}

interface LifecycleMethod {
    type: "setup" | "cleanup" | "setupSpec" | "cleanupSpec";
    range: Range;
    exists: boolean;
}

interface DataProvider {
    type: "table" | "pipe" | "combination";
    variables: string[];
    range: Range;
    rowCount?: number;
}

interface AssertionInfo {
    expression: string;
    evaluationSteps: AssertionStep[];
    passed: boolean;
    range: Range;
}

interface AssertionStep {
    expression: string;
    value: any;
    type: string;
}
```

### 使用例

```typescript
// 他の拡張機能から利用
const groovyExt = vscode.extensions.getExtension('lsp-core-team.lsp-core');
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
        
        // ブロック構造の取得
        const blocks = await api.spock.getBlockStructure(document.uri);
        console.log(`Block structure:`, blocks);
        
        // アサーション情報の取得
        const assertion = await api.spock.getAssertionInfo(document.uri, position);
        console.log(`Assertion steps:`, assertion.evaluationSteps);
    }
}
```

## コードスニペット

### Spock用スニペット

| プレフィックス | 説明 | 展開後 |
|-------------|------|--------|
| `spec` | Spock specification | 完全なSpecificationクラス |
| `feat` | Feature method | featureメソッドテンプレート |
| `given` | Given block | givenブロック |
| `whenthen` | When-Then blocks | when-thenブロックのペア |
| `expect` | Expect block | expectブロック |
| `where` | Where block with table | whereブロックとデータテーブル |
| `mock` | Mock creation | Mock()呼び出し |
| `interaction` | Mock interaction | インタラクション定義 |
| `verifyall` | VerifyAll block | verifyAllブロック |
| `setup` | Setup method | setupメソッド |
| `setupspec` | SetupSpec method | setupSpecメソッド |
| `shared` | @Shared field | @Sharedフィールド |
| `datapipe` | Data pipe | データパイプ構文 |

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

// Spock固有イベント
interface SpockEvent {
    type: "blockEntered" | "blockExited" | "interactionTriggered" | "assertionFailed";
    specification: string;
    feature?: string;
    data?: any;
}

// 使用例
api.onDidRunTests((event: TestRunEvent) => {
    if (event.type === 'finished') {
        showTestResults(event.tests);
    }
});

api.spock.onSpockEvent((event: SpockEvent) => {
    if (event.type === 'assertionFailed') {
        showAssertionDetails(event.data);
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
| `SPOCK_EXT_001` | 無効なブロック順序 |
| `SPOCK_EXT_002` | モックインタラクションエラー |
| `SPOCK_EXT_003` | データテーブル不整合 |
| `SPOCK_EXT_004` | ライフサイクルメソッドエラー |
| `SPOCK_EXT_005` | @Sharedフィールドエラー |
| `SPOCK_EXT_006` | SpockConfig読み込みエラー |