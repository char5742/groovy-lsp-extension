# VSCode拡張機能テスト

このディレクトリにはGroovy LSP VSCode拡張機能のテストが含まれています。

## 📊 現在のテストカバレッジ

最新のテストシナリオ一覧は[SCENARIOS.md](./SCENARIOS.md)を参照してください。

```bash
# シナリオ一覧を更新
bun run test:scenarios
```

## ディレクトリ構造

```
test/
├── suite/          # 単体テスト
├── integration/    # 統合テスト
├── fixtures/       # テスト用フィクスチャ
└── runTest.ts      # テストランナーエントリポイント
```

## テストの実行方法

```bash
# すべてのテストを実行
bun test

# 単体テストのみ実行
bun run test:unit

# 統合テストのみ実行
bun run test:integration
```

## テストカテゴリ

### 単体テスト (suite/)
拡張機能の個別コンポーネントをテストします。
- 拡張機能の存在確認
- アクティベーション
- 言語登録

### 統合テスト (integration/)
LSPサーバーとの通信をテストします。
- LSP接続
- テキストドキュメント同期

### フィクスチャ (fixtures/)
テストで使用するサンプルファイルを配置します。

## テスト環境

- **テストランナー**: Mocha
- **VSCode API**: @vscode/test-electron
- **タイムアウト**: 60秒（統合テストでLSPサーバー起動を待つため）

## 注意事項

- 統合テストの実行にはLSPサーバーのJARファイルがビルドされている必要があります
- テスト実行前に`bun run compile`でTypeScriptをコンパイルしてください
- LSPサーバーのログは`lsp-core/server.log`に出力されます