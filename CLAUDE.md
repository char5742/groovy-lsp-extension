# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Groovy LSP ExtensionはGroovy言語のLanguage Server Protocol (LSP)実装で、特にSpockテストフレームワークのサポートに重点を置いています。モノレポ構成でLSPコア（Java）とVSCode拡張機能（TypeScript）を含みます。

現在はPhase 4（情報提供機能）のホバー機能改善に取り組んでいます。詳細な開発計画はMILESTONES.mdを参照してください。

## アーキテクチャ

### LSPコア（Java）
- **アーキテクチャ**: オニオンアーキテクチャ（domain → application → infrastructure → presentation）
- **言語**: Java 23+（プレビュー機能有効）
- **関数型プログラミング**: Vavrライブラリ使用（Eitherモナドでエラーハンドリング、try-catch禁止）
- **Null安全性**: JSpecify使用（@NullMarkedでデフォルトnon-null）
- **DI**: Daggerによるコンパイルタイム依存性注入

### VSCode拡張機能（TypeScript）
- **言語**: TypeScript（strictモード有効）
- **Node.js**: 18以上が必要
- **非同期処理**: async/awaitパターンを使用（コールバック禁止）
- **ビルドツール**: esbuild
- **リンター/フォーマッター**: Biome

### サポートファイル拡張子
- `.groovy`, `.gradle`, `.gvy`, `.gy`, `.gsh`

## ビルド・テストコマンド

### Java LSPコア
```bash
cd lsp-core
./gradlew build              # ビルド（Error Prone自動修正含む）
./gradlew test               # テスト実行
./gradlew test --tests "TestClassName.testMethodName"  # 単一テスト実行
./gradlew jacocoTestReport   # カバレッジレポート生成
./gradlew check              # 全静的解析実行
./gradlew spotlessApply      # コード自動フォーマット
./gradlew shadowJar          # 実行可能JARビルド（build/libs/lsp-core-*-all.jarを生成）
./gradlew run                # LSPサーバーを標準I/Oモードで起動
```

### VSCode拡張機能
```bash
cd vscode-extension
npm install                  # 依存関係インストール
npm run compile              # TypeScriptビルド
npm run watch                # 変更を監視して再コンパイル
npm run test                 # テスト実行（e2eテスト）
npm run test:grep            # 特定(単体)のテストを実行（例: GREP="括弧" npm run test:grep）
npm run coverage             # カバレッジレポート生成
npm run lint                 # Biomeチェック実行
npm run lint:fix             # Biome自動修正
npm run format               # Biomeフォーマット実行
```

## 開発ルール

1. **テスト駆動開発**: 機能実装前に必ずテストを作成
2. **カバレッジ**: 80%以上を維持（単体テスト70%、統合テスト20%、e2eテスト10%）
3. **静的解析**: Error Prone（NullAway含む）、Spotless、ArchUnitを使用
4. **コミット規約**: Conventional Commits形式を使用
5. **ブランチ戦略**: `main`（安定版）、`develop`（開発）、`feature/*`、`fix/*`

## LSP実装範囲

- テキスト同期、診断（エラー・警告）
- ホバー情報、定義へジャンプ、参照検索
- シンボル検索（ドキュメント/ワークスペース）
- 自動補完、シグネチャヘルプ
- コードアクション、リファクタリング
- フォーマット（ドキュメント/範囲/オンタイプ）
- Spock特化機能（データテーブル補完、ブロック構造認識、テストメソッド生成）

## プロジェクト構造

```
lsp-core/
├── src/main/java/com/groovylsp/
│   ├── domain/          # ドメイン層（ビジネスロジック）
│   ├── application/     # アプリケーション層（ユースケース）
│   ├── infrastructure/  # インフラ層（LSP実装、パーサー）
│   └── presentation/    # プレゼンテーション層（LSPサーバー）
vscode-extension/
├── src/
│   ├── client/          # LSPクライアント
│   ├── commands/        # コマンド実装
│   ├── providers/       # 各種プロバイダー
│   └── test/
│       ├── e2e/         # e2eテスト（mocha）
│       ├── unit/        # 単体テスト（未使用）
│       └── suite/       # 拡張機能テスト（未使用）
```

## 重要な設計方針

1. **Vavrの使用**: 全てのエラーハンドリングはEitherモナドで実装（例外スローは禁止）
2. **JSpecify**: ルートレベルで`@NullMarked`を宣言（デフォルトnon-null）
3. **テスト分類**: Java側では`@FastTest`（<100ms）、`@SlowTest`（>100ms）、`@IntegrationTest`を使用
4. **非同期処理**: TypeScriptではPromise/async-awaitのみ使用（コールバック禁止）

## テスト戦略

### テストの分類と責務
1. **単体テスト（Unit Test）**
   - **実装**: JUnit（Java側のlsp-core）
   - **対象**: 個々のクラスやメソッドの振る舞い
   - **カバレッジ目標**: 70%

2. **統合テスト（Integration Test）**
   - **実装**: JUnit（Java側のlsp-core）
   - **対象**: 複数のコンポーネント間の連携
   - **カバレッジ目標**: 20%

3. **e2eテスト（End-to-End Test）**
   - **実装**: Mocha（TypeScript側のvscode-extension）
   - **対象**: VSCode拡張機能とLSPサーバー間の実際の通信
   - **カバレッジ目標**: 10%

## コーディングガイドライン

- **警告抑制**: @SuppressWarningsは極力使用しないでください
- **Gitフック**: Lefthookによる自動チェック（--no-verify禁止）
  - pre-commit: 静的解析、フォーマット、テスト実行
  - pre-push: テストシナリオ自動生成
- **静的解析エラー**: Error ProneとNullAwayのエラーは必ず修正
- **TypeScript設定**: tsconfig.jsonでstrictモード有効（ES2022ターゲット）
- **言語設定**: .github/copilot-instructions.mdに従い日本語を使用

## ツール関連

- githubの操作はghコマンドで行うようにしてください

## 開発作業ガイドライン

- 作業が完了した際は、作業範囲のテストを実行し成果物に問題がないことを必ず確認してください。
- VSCode拡張機能のデバッグ時はF5キーで新しいVSCodeウィンドウを起動し、`.groovy`ファイルを開いて拡張機能を有効化してください。
- LSPサーバーとVSCode拡張機能のe2eテスト前に、必ず`lsp-core/`で`./gradlew shadowJar`を実行してください。

## 外部ツール設定

- **Biome**: biome-ignoreを禁止します
  - フォーマット: プリントサイズ120、インデント幅2（タブではなくスペース）
  - リンター: 推奨ルールセット使用
- **カバレッジツール**: 
  - Java側: JaCoCo（HTML/XMLレポート生成）
  - TypeScript側: C8（c8レポートはcoverageディレクトリに出力）

## デバッグ・トラブルシューティング

### VSCode拡張機能の接続確認
1. VSCodeの出力パネルを開く（表示 → 出力）
2. ドロップダウンから「Groovy Language Server」を選択
3. 以下のメッセージが表示されることを確認:
   - "Groovy Language Server extension is activating..."
   - "Language client state changed: stopped -> starting"
   - "Groovy Language Server started successfully"

### LSPサーバーの手動テスト
標準入力にJSON-RPCメッセージを送信してテスト可能。初期化リクエストの例:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "processId": null,
    "capabilities": {},
    "rootUri": "file:///path/to/workspace"
  }
}
```

## Null安全性について

本プロジェクトはデフォルトNonNullです。nullに対して意識する必要はないため、nullチェックやnullに関するテストを書かないようにしてください。

## デバッグ用ログファイル

開発中に以下のログファイルが生成されます：
- `test-output.log`: テスト実行時の標準出力
- `test-hover-debug.log`: ホバー機能のデバッグログ
- VSCode拡張機能ログ: 出力パネルの「Groovy Language Server」から確認可能