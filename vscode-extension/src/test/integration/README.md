# 統合テストスイート

このディレクトリにはLSPサーバーとVSCode拡張機能の統合テストが含まれています。

## テストファイル

### lsp-connection.test.ts
LSPサーバーとの基本的な接続をテストします。

### document-sync.test.ts
テキストドキュメント同期機能をテストします。

## テストシナリオ

### LSP接続テスト (lsp-connection.test.ts)

#### 1. LSPサーバーの初期化
**目的**: LSPサーバーが`initialize`リクエストに正しく応答するか確認  
**手順**:
1. LSPサーバープロセスを起動
2. JSON-RPC形式で`initialize`リクエストを送信
3. レスポンスを受信して解析

**期待結果**:
- レスポンスのIDがリクエストIDと一致
- `result`オブジェクトが存在
- `result.capabilities`が定義されている
- タイムアウト（5秒）しない

### ドキュメント同期テスト (document-sync.test.ts)

#### 1. 完全なドキュメント同期フロー
**目的**: テキストドキュメントの同期が正常に動作するか確認  
**手順**:
1. LSPサーバーを初期化
   - `textDocument.synchronization`能力を宣言
2. `initialized`通知を送信
3. ドキュメントを開く (`textDocument/didOpen`)
   - URI: `file:///test/example.groovy`
   - 初期内容: 基本的なGroovyクラス
4. ドキュメントを変更 (`textDocument/didChange`)
   - バージョンを2に更新
   - 内容を変更
5. ドキュメントを閉じる (`textDocument/didClose`)

**期待結果**:
- 初期化レスポンスで`textDocumentSync: 1` (Full sync)が返される
- 各リクエストがエラーなく処理される
- サーバーログに適切なメッセージが出力される

## テスト実行

```bash
# 統合テストのみ実行
npm run test:integration

# 事前準備: LSPサーバーのビルド
cd ../lsp-core
./gradlew shadowJar
```

## ログ確認

テスト実行時のLSPサーバーログ:
```bash
# サーバーログの確認
tail -f ../lsp-core/server.log
```

期待されるログ出力:
```
Opening document: file:///test/example.groovy (version: 1)
Changing document: file:///test/example.groovy (version: 2)
Closing document: file:///test/example.groovy
```

## トラブルシューティング

### テストがタイムアウトする場合
1. LSPサーバーのJARファイルが存在するか確認
2. Javaランタイムが利用可能か確認
3. ポートが他のプロセスで使用されていないか確認

### レスポンスが期待と異なる場合
1. LSPサーバーのログでエラーを確認
2. JSON-RPCメッセージのフォーマットを確認
3. Content-Lengthヘッダーが正しいか確認