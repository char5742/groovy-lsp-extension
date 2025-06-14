# ADR-0003: Vavrを使用した関数型プログラミングスタイルの採用

## ステータス

承認済み（2024-06-14更新: JSpecifyとの併用方針を追加）

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

従来のtry-catchベースのエラーハンドリングの代わりに、Eitherモナドを使用してエラーを型として明示的に扱います。これによりエラー処理の漏れをコンパイル時に検出できます。

### 2. コンポーザビリティ
- モナディックな操作により、処理の連鎖が明確
- エラーの伝播が自動的に行われる
- 各ステップが独立してテスト可能

### 3. 型安全性
- コンパイル時にエラー処理の漏れを検出
- Null Pointer Exceptionの排除
- 意図が型シグネチャに現れる

### 4. ステップベースの処理

モナディックな操作により、複数の処理ステップを連鎖させ、エラーが発生した場合は自動的に伝搬されるようにします。

## 実装ガイドライン

### Tryモナドの使用

I/O操作や外部リソースアクセスにおいて、例外を値として扱います。

### Optionモナドの使用

null可能性のある値を明示的に表現し、Null Pointer Exceptionを防ぎます。

### Eitherモナドの使用

ビジネスエラーを型として表現し、正常系と異常系の両方を型安全に扱います。

### パターンマッチング

Vavrのfoldメソッドなどを使用して、値の状態に応じた処理を簡潔に記述します。

## エラー型の設計

sealed interfaceを使用してドメイン層とアプリケーション層のエラーを型として定義し、レイヤー間での適切な変換を行います。

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

## JSpecifyとの併用方針（2024-06-14追加）

ADR-0004でJSpecifyを採用したことに伴い、VavrとJSpecifyの使い分けを明確化します。

### 基本原則

1. **ビジネスロジック**: Vavrを優先使用
2. **外部API境界**: JSpecifyを使用
3. **内部では両方を適切に組み合わせる**

### 具体的な使い分け

内部ロジックではVavrの関数型データ構造を使用し、外部API（LSPプロトコル）との境界ではJSpecifyのnullabilityアノテーションを使用します。境界ではnullチェック後にVavrの型に変換するパターンを採用します。

### ガイドライン

| 状況 | 推奨アプローチ | 理由 |
|------|--------------|------|
| ドメインモデルの必須フィールド | JSpecify（デフォルトnon-null） | シンプルで効率的 |
| オプショナルなフィールド | `@Nullable` または `Option<T>` | 用途に応じて選択 |
| メソッドの戻り値（エラーあり） | `Either<Error, T>` | エラー処理の明確化 |
| メソッドの戻り値（エラーなし） | JSpecify または `Option<T>` | nullabilityの意図次第 |
| コレクション内のnull | `List<@Nullable T>` | Vavrコレクションは非null要素のみ |
| 外部ライブラリとの統合 | JSpecify | 相互運用性 |

### 移行パターン

```java
// 外部APIからの値をVavrに変換
Option<String> name = Option.of(externalApi.getName());

// Vavrの結果を外部APIに変換
@Nullable String result = option.getOrElse((String) null);

// EitherをJSpecifyスタイルに変換
public @Nullable Result process(@Nullable Input input) {
    return Option.of(input)
        .toEither(Error.NULL_INPUT)
        .flatMap(this::doProcess)
        .getOrElse((Result) null);
}

## 参考資料

- [Vavr User Guide](https://docs.vavr.io/)
- [Functional Error Handling in Java](https://www.baeldung.com/vavr-either)
- [Railway Oriented Programming](https://fsharpforfunandprofit.com/posts/recipe-part2/)