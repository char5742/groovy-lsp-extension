# Groovy LSP VSCode拡張機能

Spockフレームワークの拡張サポートを備えたGroovy Language ServerのVisual Studio Code拡張機能です。

## 機能

- VSCode用Language Clientの実装
- GroovyおよびSpockファイルタイプのサポート（`.groovy`, `.gradle`, `.gvy`, `.gy`, `.gsh`）
- LSPコアサーバーとの統合
- デバッグ用の接続ログ出力

## 開発

### 前提条件
- Node.js 18以上
- npm
- Java 23以上（LSPサーバー実行用）

### ビルドと実行

1. 依存関係のインストール:
   ```bash
   npm install
   ```

2. TypeScriptのコンパイル:
   ```bash
   npm run compile
   ```

3. LSPサーバーのビルド:
   ```bash
   cd ../lsp-core
   ./gradlew shadowJar
   ```

4. デバッグモードで拡張機能を起動:
   - このディレクトリでVSCodeを開く
   - F5キーを押して拡張機能が読み込まれた新しいVSCodeウィンドウを起動
   - `.groovy`ファイルを開いて拡張機能を有効化

### 接続のテスト

1. VSCodeの出力パネルを開く（表示 → 出力）
2. ドロップダウンから「Groovy Language Server」を選択
3. 以下のメッセージが表示されることを確認:
   - "Groovy Language Server extension is activating..."
   - "Language client state changed: stopped -> starting"
   - "Groovy Language Server started successfully"

Groovyファイルを開くと、拡張機能が自動的にLSPサーバーを起動します。

### スクリプト

- `npm run compile` - TypeScriptのコンパイル
- `npm run watch` - 変更を監視して再コンパイル
- `npm run lint` - ESLintの実行
- `npm run lint:fix` - ESLintの自動修正付き実行

### 設定

拡張機能は以下の設定オプションを提供します:

- `groovy-lsp.trace.server` - VS CodeとLanguage Server間の通信をトレース（off/messages/verbose）