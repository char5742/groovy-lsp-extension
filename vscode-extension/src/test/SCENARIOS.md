# テストシナリオ一覧

## 概要

- **総テスト数**: 43
- **単体テスト**: 8
- **統合テスト**: 35
- **E2Eテスト**: 0

## 単体テスト

### Extension Test Suite
*ファイル: src/test/suite/extension.test.ts*

- Extension should be present @core @initialization [core, initialization]
- Extension should activate @core @activation [core, activation]
- Should register Groovy language @core @language-registration [core, language]
- Extension should be present @core @initialization [core, initialization]
- Extension should activate @core @activation [core, activation]
- Should register Groovy language @core @language-registration [core, language]

### Unit Test Example
*ファイル: src/test/unit/example.test.ts*

- Simple arithmetic test
- String test

## 統合テスト

### 括弧の対応チェック機能のテスト
*ファイル: src/test/integration/bracket-matching.spec.ts*

- 正しい括弧のペアではエラーが表示されない
- 開き括弧が多い場合にエラーが表示される
- 閉じ括弧が多い場合にエラーが表示される
- 異なる種類の括弧の不一致でエラーが表示される
- ネストされた括弧が正しく対応している場合はエラーが表示されない
- 文字列内の括弧は無視される
- コメント内の括弧は無視される
- Groovy特有の構文での括弧チェック
- 複雑なSpockテストでの括弧チェック

### コマンド機能のテスト
*ファイル: src/test/integration/command.spec.ts*

- 拡張機能が正しくアクティベートされる
- groovy-lsp.restartServerコマンドが登録されている
- groovy-lsp.showOutputChannelコマンドが登録されている
- Language Clientが正しく初期化される
- 設定が正しく読み込まれる
- ステータスバーアイテムが表示される
- restartServerコマンドが実行できる

### 診断機能のテスト
*ファイル: src/test/integration/diagnostics.spec.ts*

- 固定メッセージ「Hello from Groovy LSP」が表示される (issue #5)
- ファイル変更時も固定メッセージが維持される
- 空のGroovyファイルでも固定メッセージが表示される

### Document Synchronization Test Suite
*ファイル: src/test/integration/document-sync.test.ts*

- Should handle document synchronization

### Groovyファイル判定の統合テスト
*ファイル: src/test/integration/groovy-file-detection.test.ts*

- Groovyファイルに対してのみ診断が実行される
- Gradleファイルに対して診断が実行される
- Gradle Kotlinファイルに対して診断が実行される

### Hover機能のテスト
*ファイル: src/test/integration/hover.spec.ts*

- メソッド名にホバーすると型情報が表示される
- Spockテストのwhereブロックでホバーが機能する
- Groovyのクロージャでホバーが機能する

### 字句解析の統合テスト
*ファイル: src/test/integration/lexical-analysis.test.ts*

- キーワードの字句解析が正しく動作する
- 文字列リテラルの字句解析が正しく動作する
- 数値リテラルの字句解析が正しく動作する
- 複雑なGroovyコードの字句解析が正しく動作する
- コメントの字句解析が正しく動作する
- 演算子と区切り文字の字句解析が正しく動作する

### LSP Connection Test Suite
*ファイル: src/test/integration/lsp-connection.test.ts*

- LSP server should respond to initialize request

### LSP Debug Mode Test Suite
*ファイル: src/test/integration/lsp-debug-mode.test.ts*

- LSP server should respond correctly in debug mode with quiet flag
- LSP server debug output should not interfere with protocol

