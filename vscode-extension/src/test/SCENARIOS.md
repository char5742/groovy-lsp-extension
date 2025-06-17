# テストシナリオ一覧

## 概要

- **総テスト数**: 50
- **単体テスト**: 12
- **E2Eテスト**: 38

## 単体テスト

### DocumentSymbol Test Suite
*ファイル: src/test/suite/documentSymbol.test.ts*

- Should provide document symbols for classes and methods @core @document-symbol [core, document]
- Should provide document symbols for fields and properties @core @document-symbol [core, document]
- Should provide document symbols for interfaces @core @document-symbol [core, document]
- Should handle empty files gracefully @core @document-symbol [core, document]

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

## E2Eテスト

### 構文エラーの検出
*ファイル: src/test/e2e/ast-analysis.spec.ts*

- 閉じ括弧が不足している場合、構文エラーが検出される
- 不正なインポート文がある場合、構文エラーが検出される
- 正しい構文の場合、構文エラーが検出されない

### クラス定義の認識
*ファイル: src/test/e2e/ast-analysis.spec.ts*

- 複数のクラス定義がある場合でも、各クラスが正しく認識される

### メソッド定義の認識
*ファイル: src/test/e2e/ast-analysis.spec.ts*

- Spockテストメソッドの特殊な名前が正しく認識される
- 様々なメソッド修飾子が正しく認識される

### 複雑な構文の処理
*ファイル: src/test/e2e/ast-analysis.spec.ts*

- クロージャとGStringを含むコードが正しく解析される
- Groovy特有の構文（プロパティアクセス、安全参照演算子など）が正しく解析される

### 括弧の対応チェック機能のテスト
*ファイル: src/test/e2e/bracket-matching.spec.ts*

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
*ファイル: src/test/e2e/command.spec.ts*

- 拡張機能が正しくアクティベートされる
- groovy-lsp.restartServerコマンドが登録されている
- groovy-lsp.showOutputChannelコマンドが登録されている
- Language Clientが正しく初期化される
- 設定が正しく読み込まれる
- ステータスバーアイテムが表示される
- restartServerコマンドが実行できる

### 診断機能のテスト
*ファイル: src/test/e2e/diagnostics.spec.ts*

- 行カウント情報が表示される
- ファイル変更時も行カウント情報が更新される
- 空のGroovyファイルでも行カウント情報が表示される

### Document Synchronization Test Suite
*ファイル: src/test/e2e/document-sync.test.ts*

- Should handle document synchronization

### Groovyファイル判定の統合テスト
*ファイル: src/test/e2e/groovy-file-detection.test.ts*

- Groovyファイルに対してのみ診断が実行される
- Gradleファイルに対して診断が実行される
- Gradle Kotlinファイルに対して診断が実行される

### ホバー機能のテスト
*ファイル: src/test/e2e/hover.spec.ts*

- ホバー時にGroovy要素の情報が表示される
- メソッド上でもホバー情報が表示される
- 変数上でもホバー情報が表示される
- 変数参照時に定義情報が表示される
- メソッド上でシグネチャが表示される
- クラス名上でクラス情報が表示される
- フォールバックメッセージが改善されている

