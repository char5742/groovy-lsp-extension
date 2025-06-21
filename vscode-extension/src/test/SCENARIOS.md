# テストシナリオ一覧

## 概要

- **総テスト数**: 87
- **単体テスト**: 12
- **E2Eテスト**: 75

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
*ファイル: src/test/e2e/document-sync.spec.ts*

- Should handle document synchronization

### Groovyファイル判定の統合テスト
*ファイル: src/test/e2e/groovy-file-detection.spec.ts*

- Groovyファイルに対してのみ診断が実行される
- Gradleファイルに対して診断が実行される
- Gradle Kotlinファイルに対して診断が実行される

### クロージャ変数itのホバーE2Eテスト
*ファイル: src/test/e2e/hover-closure-it.spec.ts*

- Integerリストのeachクロージャ内でitがInteger型として表示される
- Stringリストのeachクロージャ内でitがString型として表示される
- MapのeachクロージャでitがMap.Entry型として表示される
- findAllクロージャ内でitが正しい型として表示される
- カスタムクラスのリストでitが正しい型として表示される
- ネストされたクロージャで内側のitが正しい型として表示される
- timesメソッドのクロージャでitがInteger型として表示される

### ホバー機能の詳細なE2Eテスト
*ファイル: src/test/e2e/hover-detailed.spec.ts*

- メソッド呼び出しの詳細な型情報が表示される
- 変数参照時に正確な型情報が表示される
- メソッドチェーンでの型情報が正確に表示される
- Groovyの動的メソッド呼び出しでも型情報が表示される
- Mock/Stubオブジェクトの型情報が表示される

### enum関連のホバー機能E2Eテスト
*ファイル: src/test/e2e/hover-enum.spec.ts*

- enum名にホバーすると列挙子一覧が表示される
- enum定数にホバーすると型情報が表示される
- enum型フィールドにホバーすると正しい型が表示される
- パラメータなしのenum定数も正しく表示される
- ネストされたenumも正しく認識される
- switch文内のenum定数も認識される

### オブジェクト指向機能のホバーE2Eテスト
*ファイル: src/test/e2e/hover-oop.spec.ts*

- レコードクラスにホバーするとコンポーネント一覧が表示される
- ネストクラスにホバーすると完全修飾名が表示される
- オーバーライドされたメソッドにホバーすると@Overrideと親メソッド情報が表示される [Override]
- 型パラメータ境界にホバーすると上限型が表示される
- オーバーロードされたメソッド呼び出しで正しいオーバーロードが特定される
- 静的ネストクラスへのアクセスも正しく認識される
- @Canonicalアノテーション付きクラスも正しく認識される [Canonical]

### 静的メソッドとqualified呼び出しのホバーE2Eテスト
*ファイル: src/test/e2e/hover-static-method.spec.ts*

- qualified呼び出し（java.time.Instant.now()）でFQN付きシグネチャが表示される
- 静的メソッドアクセス（Math.sin(x)）でメソッド情報が表示される
- インポートされたクラスの静的メソッド（Instant.now()）でも情報が表示される
- 静的フィールド（Math.PI）でフィールド情報が表示される
- import aliasを使用した呼び出し（LD.now()）で元のクラス情報が表示される
- import alias自体（LD）にホバーすると元のクラス名が表示される

### ホバー機能の型推論E2Eテスト
*ファイル: src/test/e2e/hover-type-inference.spec.ts*

- defで宣言されたフィールドの型が初期化式から推論される
- Spockのモック生成メソッドから型が推論される
- メソッド内からフィールドを参照する際も型推論が適用される
- 明示的に型が宣言されたフィールドはその型を維持する

### ホバー機能のE2Eテスト
*ファイル: src/test/e2e/hover.spec.ts*

- クラス名にホバーした際に型情報が表示される
- メソッド名にホバーした際にシグネチャが表示される
- プロパティにホバーした際に型情報が表示される
- フィールドにホバーした際に情報が表示される
- コンストラクタにホバーした際に情報が表示される
- ローカル変数にホバーした際に型情報が表示される
- パラメータにホバーした際に型情報が表示される
- 型名（Long）にホバーした際に情報が表示される
- メソッド呼び出しにホバーした際に情報が表示される

