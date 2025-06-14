# 開発ガイド

## 開発環境セットアップ

### 必要なツール

- **Java 17+**: LSPコア開発用
- **Node.js 18+**: VSCode拡張機能開発用
- **Gradle 8+**: Javaプロジェクトのビルド
- **Git**: バージョン管理
- **VSCode**: 開発・デバッグ環境

### セットアップ手順

```bash
# リポジトリのクローン
git clone https://github.com/your-org/groovy-lsp-extension.git
cd groovy-lsp-extension

# Gitフックの設定
./scripts/setup.sh

# 依存関係のインストール
cd groovy-lsp && ./gradlew build
cd ../vscode-extension && npm install
```

## 開発フロー

### 1. ブランチ戦略

- `main`: 安定版ブランチ
- `develop`: 開発ブランチ
- `feature/*`: 機能開発
- `fix/*`: バグ修正
- `docs/*`: ドキュメント更新

### 2. コミットメッセージ

[Conventional Commits](https://www.conventionalcommits.org/)に従う：

```
<type>(<scope>): <subject>

<body>

<footer>
```

タイプ:
- `feat`: 新機能
- `fix`: バグ修正
- `docs`: ドキュメント
- `style`: フォーマット
- `refactor`: リファクタリング
- `test`: テスト
- `chore`: ビルド・ツール

### 3. TDD/BDDの実践

#### Javaコード（JUnit 5）

```java
@Test
@DisplayName("補完候補を正しく返すこと")
void shouldReturnCompletionItems() {
    // Given
    var position = new Position(10, 5);
    var context = CompletionContext.builder()
        .triggerKind(CompletionTriggerKind.Invoked)
        .build();
    
    // When
    var result = completionService.complete(document, position, context);
    
    // Then
    assertThat(result)
        .isRight()
        .extracting(CompletionList::getItems)
        .asList()
        .hasSize(3)
        .extracting("label")
        .containsExactlyInAnyOrder("given", "when", "then");
}
```

#### Mock JSON-RPC通信テスト（lsp4j）

lsp4jには`org.eclipse.lsp4j.jsonrpc.services`を使ったMock JSON-RPC通信テストのサンプルがあり、JUnitベースでプロトコル面を検証できます。

```java
@Test
@DisplayName("JSON-RPCプロトコルのテスト")
void shouldHandleJsonRpcProtocol() {
    // Given: Mock JSON-RPCエンドポイントの作成
    var mockService = mock(LanguageServer.class);
    var endpoint = ServiceEndpoints.toEndpoint(mockService);
    
    // JSON-RPCメッセージのシミュレーション
    var launcher = Launcher.createLauncher(mockService, LanguageClient.class,
        new ByteArrayInputStream(jsonRequest.getBytes()),
        new ByteArrayOutputStream());
    
    // When: リクエストの送信
    launcher.startListening();
    
    // Then: プロトコルレベルでの検証
    verify(mockService).initialize(any());
}
```

#### TypeScriptコード（Mocha/Chai）

```typescript
describe('GroovyLanguageClient', () => {
    it('should connect to language server', async () => {
        // Given
        const serverOptions = createServerOptions();
        const clientOptions = createClientOptions();
        
        // When
        const client = new LanguageClient('groovy-lsp', serverOptions, clientOptions);
        await client.start();
        
        // Then
        expect(client.state).to.equal(State.Running);
    });
});
```

## コーディング規約

### Java（LSPコア）

#### Vavrの活用

```java
// × 避けるべきコード
try {
    var result = parseGroovyFile(path);
    return result;
} catch (IOException e) {
    logger.error("Failed to parse file", e);
    return null;
}

// ○ 推奨コード
return Try.of(() -> parseGroovyFile(path))
    .onFailure(e -> logger.error("Failed to parse file", e))
    .toEither()
    .mapLeft(ErrorResponse::from);
```

#### Daggerによる依存性注入

```java
@Module
public interface LanguageServerModule {
    @Binds
    CompletionService bindCompletionService(CompletionServiceImpl impl);
    
    @Provides
    @Singleton
    static GroovyParser provideGroovyParser() {
        return new GroovyParser();
    }
}
```

### TypeScript（VSCode拡張）

#### 非同期処理

```typescript
// × 避けるべきコード
function loadConfiguration(callback: (config: Config) => void) {
    fs.readFile('config.json', (err, data) => {
        if (err) {
            callback(defaultConfig);
        } else {
            callback(JSON.parse(data.toString()));
        }
    });
}

// ○ 推奨コード
async function loadConfiguration(): Promise<Config> {
    try {
        const data = await fs.promises.readFile('config.json', 'utf-8');
        return JSON.parse(data);
    } catch {
        return defaultConfig;
    }
}
```

## ビルドとテスト

### LSPコア

```bash
cd groovy-lsp

# ビルド
./gradlew build

# テスト実行
./gradlew test

# カバレッジレポート
./gradlew jacocoTestReport

# 静的解析
./gradlew check
```

### VSCode拡張機能

```bash
cd vscode-extension

# ビルド
npm run compile

# テスト実行
npm test

# カバレッジレポート
npm run coverage

# Lint実行
npm run lint
```

## デバッグ

### LSPサーバーのデバッグ

1. VSCodeでgroovy-lspフォルダを開く
2. `.vscode/launch.json`の"Debug LSP Server"を選択
3. F5でデバッグ開始

### VSCode拡張機能のデバッグ

1. VSCodeでvscode-extensionフォルダを開く
2. F5で拡張機能開発ホストを起動
3. 開発ホストでGroovyファイルを開いてテスト

## CI/CDパイプライン

### プルリクエスト時

1. 静的解析（Error Prone, Spotless, ESLint）
2. 単体テスト実行
3. カバレッジチェック（100%必須）
4. アーキテクチャテスト（ArchUnit）

### マージ時

1. 統合テスト実行
2. E2Eテスト実行
3. ドキュメント生成
4. カバレッジレポート更新（OctoCov）

## トラブルシューティング

### よくある問題

#### Gradleビルドが失敗する

```bash
# Gradleキャッシュのクリア
./gradlew clean
rm -rf ~/.gradle/caches

# 再ビルド
./gradlew build --refresh-dependencies
```

#### VSCode拡張機能が認識されない

```bash
# node_modulesの再インストール
rm -rf node_modules package-lock.json
npm install

# VSCodeの再起動
```

## リリース手順

1. `develop`ブランチから`release/x.x.x`ブランチを作成
2. バージョン番号を更新
3. CHANGELOGを更新
4. PRを作成してレビュー
5. `main`ブランチへマージ
6. タグを作成（GitHub Actionsが自動リリース）