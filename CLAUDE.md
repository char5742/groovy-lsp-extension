# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

Groovy LSP ExtensionはGroovy言語のLanguage Server Protocol (LSP)実装で、特にSpockテストフレームワークのサポートに重点を置いています。モノレポ構成でLSPコア（Java）とVSCode拡張機能（TypeScript）を含みます。

**重要**: 現在このプロジェクトは計画・ドキュメント段階で、実装コードはまだ存在しません。

## アーキテクチャ

### LSPコア（Java）
- **アーキテクチャ**: オニオンアーキテクチャ（domain → application → infrastructure → presentation）
- **言語**: Java 23+
- **関数型プログラミング**: Vavrライブラリ使用（Eitherモナドでエラーハンドリング、try-catch禁止）
- **Null安全性**: JSpecify使用（@NullMarkedでデフォルトnon-null）
- **DI**: Daggerによるコンパイルタイム依存性注入

### VSCode拡張機能（TypeScript）
- **言語**: TypeScript（strictモード有効）
- **非同期処理**: async/awaitパターンを使用（コールバック禁止）

## ビルド・テストコマンド

### Java LSPコア
```bash
cd lsp-core
./gradlew build              # ビルド（Error Prone自動修正含む）
./gradlew test               # テスト実行
./gradlew jacocoTestReport   # カバレッジレポート生成
./gradlew check              # 全静的解析実行
./gradlew spotlessApply      # コード自動フォーマット
```

### VSCode拡張機能
```bash
cd vscode-extension
npm install                  # 依存関係インストール
npm run compile              # TypeScriptビルド
npm test                     # テスト実行
npm run coverage             # カバレッジレポート生成
npm run lint                 # Biomeチェック実行
npm run lint:fix             # Biome自動修正
npm run format               # Biomeフォーマット実行
```

## 開発ルール

1. **テスト駆動開発**: 機能実装前に必ずテストを作成
2. **カバレッジ**: 80%以上を維持（単体テスト70%、統合テスト20%、E2E10%）
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
│   └── providers/       # 各種プロバイダー
```

## 重要な設計方針

1. **Vavrの使用**: 全てのエラーハンドリングはEitherモナドで実装（例外スローは禁止）
2. **JSpecify**: 各パッケージに`package-info.java`を配置し`@NullMarked`を宣言
3. **テスト分類**: `@FastTest`（<100ms）、`@SlowTest`（>100ms）、`@IntegrationTest`を使用
4. **非同期処理**: TypeScriptではPromise/async-awaitのみ使用（コールバック禁止）

## コーディングガイドライン

- **警告抑制**: @SuppressWarningsは極力使用しないでください

## 追加ガイドライン

- セットアップ用のスクリプトは要求されたときのみ作成してください

## ツール関連

- githubの操作はghコマンドで行うようにしてください