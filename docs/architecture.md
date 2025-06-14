# Groovy LSP Extension アーキテクチャ

## 概要

本プロジェクトは、Groovy言語のためのLanguage Server Protocol (LSP) 実装とVSCode拡張機能を提供します。特に、Spockテストフレームワークの快適な記述をサポートすることを重視しています。

## プロジェクト構成

モノレポ構成を採用し、LSPコアとVSCode拡張機能を単一リポジトリで管理します。

```
groovy-lsp-extension/
├── groovy-lsp/                 # Java LSPコア実装
│   ├── domain/                 # ドメイン層
│   ├── application/            # アプリケーション層
│   ├── infrastructure/         # インフラストラクチャ層
│   └── presentation/           # プレゼンテーション層
├── vscode-extension/           # VSCode拡張機能
│   ├── src/                    # TypeScriptソース
│   └── tests/                  # テストコード
├── docs/                       # ドキュメント
│   ├── adr/                    # Architecture Decision Records
│   └── architecture.md         # 本ドキュメント
└── .github/                    # GitHub Actions設定

```

## アーキテクチャ原則

### LSPコア（Java）

- **オニオンアーキテクチャ**: 依存関係は外側から内側へ向かう
- **関数型プログラミング**: Vavrライブラリを活用し、副作用を最小化
- **エラーハンドリング**: try-catchを避け、Eitherモナドでエラーを表現
- **依存性注入**: Daggerを使用したコンパイルタイムDI

### VSCode拡張機能（TypeScript）

- **シンプルな設計**: LSPクライアントとしての責務に集中
- **ユーザビリティ重視**: Spock記述時の開発体験を最適化

## LSP機能範囲

以下の全てのLSP機能を実装します：

1. **基本機能**
   - テキスト同期
   - 診断（エラー・警告）
   - ホバー情報

2. **ナビゲーション**
   - 定義へジャンプ
   - 参照検索
   - シンボル検索（ドキュメント/ワークスペース）

3. **編集支援**
   - 自動補完
   - シグネチャヘルプ
   - コードアクション
   - リファクタリング

4. **フォーマット**
   - ドキュメント全体フォーマット
   - 範囲フォーマット
   - オンタイプフォーマット

5. **Spock特化機能**
   - データテーブル補完
   - ブロック構造の認識
   - テストメソッド生成

## 品質保証

### テスト戦略

- **TDD/BDD**: 全ての機能は振る舞いを先に定義
- **カバレッジ**: 80%を常に維持
- **E2Eテスト**: 常にパスする状態を保持

### CI/CDパイプライン

GitHub Actionsで以下を実行：

1. **静的解析**
   - Error Prone（NullAway含む）
   - Spotless（フォーマット）
   - ArchUnit（アーキテクチャ検証）

2. **テスト実行**
   - 単体テスト（JUnit 5）
   - 統合テスト
   - E2Eテスト

3. **カバレッジ計測**
   - JaCoCo（Java）
   - C8（TypeScript）
   - OctoCov（可視化）

### ローカル開発環境

- **Lefthook**: Gitフックでの品質チェック
- **--no-verifyの禁止**: エイリアス設定で強制
- **TODO管理**: todo-to-issueで自動Issue化