# VSCode拡張機能テスト

このディレクトリにはGroovy LSP VSCode拡張機能のテストが含まれています。

## 📊 現在のテストカバレッジ

最新のテストシナリオ一覧は[SCENARIOS.md](./SCENARIOS.md)を参照してください。

```bash
# シナリオ一覧を更新
npm run test:scenarios
```

## ディレクトリ構造

```
test/
├── suite/          # 単体テスト
├── e2e/            # e2eテスト
├── fixtures/       # テスト用フィクスチャ
└── runTest.ts      # テストランナーエントリポイント
```

## テストの実行方法

```bash
# すべてのテストを実行（e2eテスト）
npm run test

# シナリオ一覧を更新
npm run test:scenarios
```

## テストカテゴリ

### 単体テスト (suite/)
拡張機能の個別コンポーネントをテストします。
- 拡張機能の存在確認
- アクティベーション
- 言語登録

### e2eテスト (e2e/)
LSPサーバーとの通信をテストします。
- LSP接続
- テキストドキュメント同期
- AST解析
- 診断機能
- 括弧の対応チェック

### フィクスチャ (fixtures/)
テストで使用するサンプルファイルを配置します。

## テスト環境

- **テストランナー**: Mocha
- **VSCode API**: @vscode/test-electron
- **タイムアウト**: 60秒（e2eテストでLSPサーバー起動を待つため）

## 注意事項

- e2eテストの実行にはLSPサーバーのJARファイルがビルドされている必要があります
- テスト実行前に`npm run compile`でTypeScriptをコンパイルしてください
- LSPサーバーのログは`lsp-core/server.log`に出力されます