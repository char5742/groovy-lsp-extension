# Groovy Language Server Protocol仕様

## 概要

本ドキュメントは、Groovy Language Serverが実装するLanguage Server Protocol (LSP) の仕様を定義します。標準LSP仕様に加えて、Groovy/Spock特有の拡張機能についても記載します。

## サーバー情報

```typescript
interface ServerInfo {
    name: "groovy-language-server"
    version: string  // セマンティックバージョニング
}
```

## 初期化

### InitializeParams

```typescript
interface GroovyInitializeParams extends InitializeParams {
    initializationOptions?: {
        // Groovy SDKのパス
        groovyHome?: string
        
        // Spock機能の有効化
        spockSupport?: boolean
        
        // JVMオプション
        jvmOptions?: string[]
        
        // ワークスペースのライブラリパス
        classpath?: string[]
        
        // 静的型チェックのレベル
        typeCheckingLevel?: "none" | "static" | "dynamic"
    }
}
```

### ServerCapabilities

```typescript
interface GroovyServerCapabilities extends ServerCapabilities {
    // 標準LSP機能
    textDocumentSync: TextDocumentSyncKind.Incremental
    completionProvider: {
        resolveProvider: true
        triggerCharacters: [".", ":", "@", "(", "[", "{", " "]
        // Spockキーワード補完
        contextSupport: true
    }
    hoverProvider: true
    signatureHelpProvider: {
        triggerCharacters: ["(", ","]
        retriggerCharacters: [","]
    }
    definitionProvider: true
    typeDefinitionProvider: true
    implementationProvider: true
    referencesProvider: true
    documentHighlightProvider: true
    documentSymbolProvider: true
    workspaceSymbolProvider: true
    codeActionProvider: {
        codeActionKinds: [
            "quickfix",
            "refactor",
            "refactor.extract",
            "refactor.inline",
            "refactor.rewrite",
            "source.organizeImports",
            "source.generateTest"  // Spockテスト生成
        ]
    }
    codeLensProvider: {
        resolveProvider: true
    }
    documentFormattingProvider: true
    documentRangeFormattingProvider: true
    documentOnTypeFormattingProvider: {
        firstTriggerCharacter: "}"
        moreTriggerCharacter: [";", "\n"]
    }
    renameProvider: {
        prepareProvider: true
    }
    foldingRangeProvider: true
    executeCommandProvider: {
        commands: [
            "groovy.generateGetter",
            "groovy.generateSetter",
            "groovy.generateConstructor",
            "groovy.convertToStatic",
            "groovy.runTest",
            "groovy.debugTest",
            // Spock特有コマンド
            "groovy.spock.generateDataTable",
            "groovy.spock.addTestCase",
            "groovy.spock.convertToParameterized",
            "groovy.spock.generateMock",
            "groovy.spock.addInteraction",
            "groovy.spock.convertToVerifyAll",
            "groovy.spock.generateLifecycleMethod",
            "groovy.spock.showAssertionVisualization",
        ]
    }
    
    // カスタム機能
    experimental?: {
        spockFeatures?: {
            dataTableCompletion: boolean
            blockValidation: boolean
            whereBlockAssist: boolean
            mockInteractionCompletion: boolean
            assertionVisualization: boolean
            sharedFieldValidation: boolean
            dataPipeCompletion: boolean
            annotationSupport: boolean
            verifyAllSupport: boolean
        }
    }
}
```

## 標準LSPメソッド

### textDocument/completion

#### リクエスト
```typescript
interface CompletionParams {
    textDocument: TextDocumentIdentifier
    position: Position
    context?: CompletionContext
}
```

#### レスポンス
```typescript
interface CompletionList {
    isIncomplete: boolean
    items: CompletionItem[]
}

interface GroovyCompletionItem extends CompletionItem {
    label: string
    kind: CompletionItemKind
    detail?: string
    documentation?: string | MarkupContent
    sortText?: string
    filterText?: string
    insertText?: string
    insertTextFormat?: InsertTextFormat
    additionalTextEdits?: TextEdit[]
    
    // Groovy特有
    data?: {
        type: "method" | "field" | "class" | "spock-keyword" | "spock-block"
        groovyType?: string  // 完全修飾型名
        isStatic?: boolean
        isDeprecated?: boolean
        spockContext?: "given" | "when" | "then" | "expect" | "where" | "and" | "cleanup"
        mockType?: "mock" | "stub" | "spy" | "interaction"
    }
}
```

### textDocument/hover

#### レスポンス
```typescript
interface Hover {
    contents: MarkupContent
    range?: Range
}

// Groovy向けホバー内容
interface GroovyHoverContent {
    // 型情報
    type?: string
    
    // Groovydoc
    documentation?: string
    
    // サンプルコード
    examples?: string[]
    
    // Spock関連情報
    spockInfo?: {
        blockType?: string
        dataVariables?: string[]
        assertionSteps?: AssertionStep[]
        mockInteractions?: MockInteraction[]
        sharedFields?: string[]
    }
}

interface AssertionStep {
    expression: string
    value: any
    passed: boolean
}

interface MockInteraction {
    mock: string
    method: string
    cardinality: string
    arguments: string[]
}
```

### textDocument/codeAction

#### カスタムコードアクション

```typescript
interface GroovyCodeAction extends CodeAction {
    title: string
    kind: CodeActionKind
    diagnostics?: Diagnostic[]
    edit?: WorkspaceEdit
    command?: Command
    
    // Groovy特有のアクション
    data?: {
        // Spockテスト生成時のオプション
        testFramework?: "spock" | "junit"
        testStyle?: "bdd" | "classic"
        
        // リファクタリングオプション
        refactorType?: "extractMethod" | "extractVariable" | "inline"
    }
}
```

## カスタムリクエスト

### groovy/resolveClasspath

ワークスペースのクラスパスを解決します。

#### リクエスト
```typescript
interface ResolveClasspathParams {
    uri: string  // プロジェクトルートURI
}
```

#### レスポンス
```typescript
interface ResolveClasspathResult {
    entries: ClasspathEntry[]
}

interface ClasspathEntry {
    path: string
    kind: "source" | "library" | "output"
    exported?: boolean
}
```

### groovy/spock/getAssertionVisualization

アサーションの評価ステップを取得します。

#### リクエスト
```typescript
interface GetAssertionVisualizationParams {
    textDocument: TextDocumentIdentifier
    position: Position  // アサーションの位置
}
```

#### レスポンス
```typescript
interface AssertionVisualization {
    expression: string
    steps: Array<{
        expression: string
        value: any
        type: string
        passed?: boolean
    }>
    passed: boolean
    range: Range
}
```

### groovy/ast

指定位置のAST情報を取得します（デバッグ用）。

#### リクエスト
```typescript
interface AstParams {
    textDocument: TextDocumentIdentifier
    position: Position
}
```

#### レスポンス
```typescript
interface AstResult {
    node: AstNode
}

interface AstNode {
    type: string
    range: Range
    children?: AstNode[]
    properties?: Record<string, any>
}
```

## 診断（Diagnostics）

### エラーコード

```typescript
enum GroovyDiagnosticCode {
    // 構文エラー (1000-1999)
    SYNTAX_ERROR = 1000,
    MISSING_SEMICOLON = 1001,
    UNCLOSED_STRING = 1002,
    
    // 型エラー (2000-2999)
    TYPE_MISMATCH = 2000,
    UNDEFINED_VARIABLE = 2001,
    UNDEFINED_METHOD = 2002,
    INCOMPATIBLE_TYPES = 2003,
    
    // Groovy特有 (3000-3999)
    MISSING_PROPERTY = 3000,
    INVALID_CLOSURE = 3001,
    GSTRING_TYPE_ERROR = 3002,
    
    // Spock特有 (4000-4999)
    INVALID_BLOCK_ORDER = 4000,
    MISSING_THEN_BLOCK = 4001,
    WHERE_BLOCK_ERROR = 4002,
    DATA_TABLE_MISMATCH = 4003,
    INVALID_MOCK_INTERACTION = 4004,
    MISSING_MOCK_DEFINITION = 4005,
    INVALID_LIFECYCLE_METHOD = 4006,
    SHARED_FIELD_ERROR = 4007,
    INVALID_SPOCK_ANNOTATION = 4008,
    DATA_PIPE_ERROR = 4009,
    VERIFY_ALL_ERROR = 4010,
    
    // 警告 (5000-5999)
    DEPRECATED_API = 5000,
    UNUSED_IMPORT = 5001,
    UNUSED_VARIABLE = 5002
}
```

### 診断メッセージフォーマット

```typescript
interface GroovyDiagnostic extends Diagnostic {
    range: Range
    severity: DiagnosticSeverity
    code?: GroovyDiagnosticCode
    source: "groovy-lsp"
    message: string
    
    // 追加情報
    data?: {
        // 修正候補
        suggestions?: string[]
        
        // 関連するシンボル
        relatedSymbols?: string[]
        
        // Spock関連
        spockContext?: {
            block: string
            issue: string
            featureMethod?: string
            mockInfo?: {
                mockName: string
                expectedInteractions: number
                actualInteractions: number
            }
        }
    }
}
```

## 設定スキーマ

```typescript
interface GroovyLanguageServerSettings {
    // 基本設定
    "groovy.home": string
    "groovy.classpath": string[]
    "groovy.encoding": string  // デフォルト: "UTF-8"
    
    // 言語機能
    "groovy.typeChecking.level": "none" | "static" | "dynamic"
    "groovy.completion.autoImport": boolean
    "groovy.completion.guessMethodArguments": boolean
    
    // フォーマット
    "groovy.format.enabled": boolean
    "groovy.format.style": "default" | "google" | "custom"
    "groovy.format.configPath": string
    
    // Spock設定
    "groovy.spock.enabled": boolean
    "groovy.spock.dataTableAlignment": boolean
    "groovy.spock.mockValidation": boolean
    "groovy.spock.assertionVisualization": boolean
    "groovy.spock.sharedFieldValidation": boolean
    "groovy.spock.annotationValidation": boolean
    "groovy.spock.verifyAllSupport": boolean
    "groovy.spock.dataPipeCompletion": boolean
    "groovy.spock.configFile": string  // SpockConfig.groovyのパス
    "groovy.spock.parallelExecution": boolean
    "groovy.spock.parallelMode": "methods" | "classes" | "same_thread"
    
    // 診断
    "groovy.diagnostic.enable": boolean
    "groovy.diagnostic.deprecatedWarnings": boolean
    "groovy.diagnostic.unusedWarnings": boolean
    
    // JVM設定
    "groovy.jvm.maxHeapSize": string  // 例: "2G"
    "groovy.jvm.additionalOptions": string[]
}
```

## エラーレスポンス

すべてのエラーは以下の形式で返されます：

```typescript
interface ErrorResponse {
    code: number
    message: string
    data?: {
        // エラーの詳細情報
        detail?: string
        
        // スタックトレース（デバッグモード時のみ）
        stackTrace?: string
        
        // 回復可能なアクション
        actions?: Array<{
            title: string
            command: string
        }>
    }
}
```

## Spock特有の通知

### spock/testRunProgress

データ駆動テストの進捗を通知します。

```typescript
interface TestRunProgressParams {
    specification: string
    feature: string
    iteration: number
    totalIterations: number
    dataVariables: Record<string, any>
    status: "running" | "passed" | "failed" | "skipped"
}
```

### spock/mockInteraction

モックインタラクションの発生を通知します。

```typescript
interface MockInteractionParams {
    mock: string
    method: string
    arguments: any[]
    returnValue: any
    timestamp: number
    location: Location
}
```

## プロトコルバージョン

- LSP Version: 3.17.0
- Groovy LSP Version: 1.0.0
- 最小クライアントバージョン: VSCode 1.75.0