# プロジェクト構造詳細

## ディレクトリ構成

```
groovy-lsp-extension/
├── lsp-core/                          # Java LSPコア
│   ├── build.gradle                     # Gradleビルド設定
│   ├── settings.gradle                  # Gradle設定
│   ├── gradle.properties                # Gradleプロパティ
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/groovylsp/
│   │   │   │       ├── domain/          # ドメイン層
│   │   │   │       │   ├── model/       # ドメインモデル
│   │   │   │       │   ├── repository/  # リポジトリインターフェース
│   │   │   │       │   └── service/     # ドメインサービス
│   │   │   │       ├── application/     # アプリケーション層
│   │   │   │       │   ├── usecase/     # ユースケース
│   │   │   │       │   └── service/     # アプリケーションサービス
│   │   │   │       ├── infrastructure/  # インフラストラクチャ層
│   │   │   │       │   ├── lsp/         # LSP実装
│   │   │   │       │   ├── parser/      # Groovyパーサー
│   │   │   │       │   └── repository/  # リポジトリ実装
│   │   │   │       └── presentation/    # プレゼンテーション層
│   │   │   │           └── server/      # LSPサーバー
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       ├── java/                    # テストコード
│   │       └── resources/               # テストリソース
│   └── config/
│       ├── checkstyle/                  # Checkstyle設定
│       ├── spotless/                    # Spotless設定
│       └── errorprone/                  # Error Prone設定
│
├── vscode-extension/                    # VSCode拡張機能
│   ├── package.json                     # NPMパッケージ設定
│   ├── tsconfig.json                    # TypeScript設定
│   ├── .eslintrc.json                   # ESLint設定
│   ├── .prettierrc                      # Prettier設定
│   ├── src/
│   │   ├── extension.ts                 # エントリーポイント
│   │   ├── client/                      # LSPクライアント
│   │   ├── commands/                    # コマンド実装
│   │   ├── providers/                   # 各種プロバイダー
│   │   └── utils/                       # ユーティリティ
│   ├── test/
│   │   ├── unit/                        # 単体テスト
│   │   ├── e2e/                         # e2eテスト
│   │   └── suite/                       # テストスイート
│   └── resources/
│       ├── icons/                       # アイコン
│       └── snippets/                    # スニペット
│
├── docs/                                # ドキュメント
│   ├── adr/                             # Architecture Decision Records
│   │   ├── 0001-use-monorepo.md
│   │   ├── 0002-onion-architecture.md
│   │   └── ...
│   ├── architecture.md                  # アーキテクチャ概要
│   ├── project-structure.md             # 本ドキュメント
│   ├── development-guide.md             # 開発ガイド
│   └── api/                             # API仕様書
│
├── scripts/                             # 開発用スクリプト
│   ├── setup.sh                         # 環境セットアップ
│   ├── build.sh                         # ビルドスクリプト
│   └── release.sh                       # リリーススクリプト
│
├── .github/
│   ├── workflows/                       # GitHub Actions
│   │   ├── ci.yml                      # CI設定
│   │   ├── release.yml                  # リリース設定
│   │   └── todo-to-issue.yml           # TODO管理
│   ├── ISSUE_TEMPLATE/                  # Issueテンプレート
│   └── PULL_REQUEST_TEMPLATE.md         # PRテンプレート
│
├── .lefthook.yml                        # Lefthook設定
├── .gitignore                           # Git除外設定
├── README.md                            # プロジェクト説明
└── LICENSE                              # ライセンス
```

## 各層の責務

### ドメイン層
- ビジネスロジックとルールを含む
- 外部依存を持たない純粋なJavaコード
- Groovy言語の構造をモデル化
- @NullMarkedでデフォルトnon-null

### アプリケーション層
- ユースケースを実装
- ドメイン層のオーケストレーション
- トランザクション境界の管理

### インフラストラクチャ層
- LSPプロトコルの実装
- Groovyソースコードのパース
- ファイルシステムへのアクセス

### プレゼンテーション層
- LSPサーバーのエンドポイント
- リクエスト/レスポンスの変換
- 通信プロトコルの管理

## 命名規則

### Java（LSPコア）
- パッケージ名: `com.groovylsp.{layer}.{feature}`
- クラス名: PascalCase
- メソッド名: camelCase
- 定数: UPPER_SNAKE_CASE

### TypeScript（VSCode拡張）
- ファイル名: kebab-case.ts
- クラス名: PascalCase
- 関数名: camelCase
- 定数: UPPER_SNAKE_CASE

## 設定ファイル

### Gradle設定
- マルチプロジェクトビルドは使用しない（単一プロジェクト）
- 依存関係はcatalogで一元管理

### TypeScript設定
- strictモードを有効化
- ESModulesを使用
- ソースマップ生成を有効化

## パッケージレベルの設定

各パッケージには適切な設定ファイルを配置し、Null安全性を担保する設計を適用します。