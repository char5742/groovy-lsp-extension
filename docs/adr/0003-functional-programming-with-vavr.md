# ADR-0003: Vavrを使用した関数型プログラミングスタイルの採用

## ステータス

承認済み

## コンテキスト

LSPコアの実装において、以下の課題に対処する必要があります：
- エラーハンドリングの一貫性
- Null安全性の確保
- 副作用の管理と予測可能性
- テスタビリティの向上

Javaの標準機能だけでは、これらの課題に対して十分な解決策を提供できません。

## 決定

[Vavr](https://www.vavr.io/)ライブラリを使用して、関数型プログラミングスタイルを採用します。特に以下の方針を定めます：

1. **try-catchの原則禁止**: Tryモナドを使用
2. **nullの原則禁止**: Optionモナドを使用
3. **エラー表現**: Eitherモナドを使用
4. **不変データ構造**: Vavrのコレクションを活用

## 根拠

### 1. 明示的なエラーハンドリング
```java
// × 従来のアプローチ
public CompletionList complete(String uri, Position pos) {
    try {
        Document doc = documentService.getDocument(uri);
        if (doc == null) {
            return new CompletionList(Collections.emptyList());
        }
        return analyzer.analyze(doc, pos);
    } catch (IOException e) {
        logger.error("Failed to complete", e);
        return new CompletionList(Collections.emptyList());
    }
}

// ○ Vavrを使用したアプローチ
public Either<CompletionError, CompletionList> complete(String uri, Position pos) {
    return documentService.getDocument(uri)
        .toEither(CompletionError.documentNotFound(uri))
        .flatMap(doc -> analyzer.analyze(doc, pos));
}
```

### 2. コンポーザビリティ
- モナディックな操作により、処理の連鎖が明確
- エラーの伝播が自動的に行われる
- 各ステップが独立してテスト可能

### 3. 型安全性
- コンパイル時にエラー処理の漏れを検出
- Null Pointer Exceptionの排除
- 意図が型シグネチャに現れる

### 4. ステップベースの処理
```java
public Either<ValidationError, ProcessedDocument> processDocument(String content) {
    return validateSyntax(content)
        .flatMap(this::parseDocument)
        .flatMap(this::resolveSymbols)
        .flatMap(this::performTypeCheck)
        .map(this::optimizeAst);
}
```

## 実装ガイドライン

### Tryモナドの使用
```java
// I/O操作や外部リソースアクセス
public Try<String> readFile(Path path) {
    return Try.of(() -> Files.readString(path));
}
```

### Optionモナドの使用
```java
// Nullableな値の表現
public Option<Symbol> findSymbol(String name) {
    return Option.of(symbolTable.get(name));
}
```

### Eitherモナドの使用
```java
// ビジネスエラーの表現
public Either<ParseError, GroovyAst> parse(String source) {
    return parser.parse(source)
        .toEither()
        .mapLeft(ParseError::from);
}
```

### パターンマッチング
```java
// Vavrのパターンマッチングを活用
public String formatMessage(Either<Error, Result> either) {
    return either.fold(
        error -> "Error: " + error.getMessage(),
        result -> "Success: " + result.getValue()
    );
}
```

## エラー型の設計

```java
// ドメイン層
public sealed interface DomainError {
    record ParseError(String message, Position position) implements DomainError {}
    record ValidationError(List<String> violations) implements DomainError {}
}

// アプリケーション層
public sealed interface ApplicationError {
    record NotFound(String resource) implements ApplicationError {}
    record InvalidRequest(String reason) implements ApplicationError {}
    
    static ApplicationError from(DomainError error) {
        return new InvalidRequest(error.toString());
    }
}
```

## 影響

### ポジティブな影響
- エラー処理の一貫性向上
- ランタイムエラーの大幅な削減
- コードの意図が明確に
- 関数の合成が容易
- テストコードがシンプルに

### ネガティブな影響
- 学習曲線（関数型概念の理解が必要）
- スタックトレースが読みにくくなる可能性
- 既存のJavaライブラリとの相互運用で変換が必要

### 軽減策
- チーム内での関数型プログラミング勉強会
- 共通のユーティリティクラスで変換処理を提供
- デバッグ時のログ出力を充実させる

## 例外的なケース

以下の場合のみ、従来のtry-catchを許可：
1. メインメソッドでの最終的なエラーハンドリング
2. スレッドの最上位でのキャッチオール
3. 外部ライブラリとの境界（ただし即座にTryに変換）

## 参考資料

- [Vavr User Guide](https://docs.vavr.io/)
- [Functional Error Handling in Java](https://www.baeldung.com/vavr-either)
- [Railway Oriented Programming](https://fsharpforfunandprofit.com/posts/recipe-part2/)