# スクリプト

## generate-test-scenarios.ts

テストコードを静的解析して、テストシナリオ一覧を自動生成するスクリプトです。

### 実行方法

```bash
npm run test:scenarios
```

### 出力ファイル

- `src/test/SCENARIOS.md` - Markdown形式のシナリオ一覧（人間が読むため）
- `src/test/scenarios.json` - JSON形式のシナリオデータ（ツール連携用）

### 機能

1. **テストファイルの自動検出**
   - `*.test.ts`と`*.spec.ts`ファイルを再帰的に検索
   
2. **メタデータ抽出**
   - suite/describe名
   - test/it名
   - タグ（`@tag`形式）
   - ファイルパス
   - カテゴリ（unit/integration/e2e）

3. **統計情報**
   - 総テスト数
   - カテゴリ別テスト数

### タグの使い方

テスト名にタグを追加することで、テストを分類できます：

```typescript
test('Should authenticate user @auth @smoke', async () => {
  // テスト実装
});
```

利用可能なタグ例：
- `@core` - コア機能
- `@auth` - 認証関連
- `@smoke` - スモークテスト
- `@regression` - リグレッションテスト
- `@performance` - パフォーマンステスト

### CI/CDでの活用

GitHub Actionsなどで自動実行し、PRにコメントとして投稿できます：

```yaml
- name: Generate test scenarios
  run: npm run test:scenarios
  
- name: Upload scenarios
  uses: actions/upload-artifact@v3
  with:
    name: test-scenarios
    path: src/test/SCENARIOS.md
```

### 今後の拡張案

1. **JSDocコメントからの詳細情報抽出**
2. **カバレッジ情報との統合**
3. **機能要件との紐付け**
4. **視覚的なダッシュボード生成**