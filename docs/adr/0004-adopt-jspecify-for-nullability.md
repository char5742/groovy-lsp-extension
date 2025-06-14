# ADR-0004: JSpecifyによるnullability管理の採用

## ステータス

承認済み

## コンテキスト

JavaプロジェクトにおけるNull Pointer Exception（NPE）の防止は重要な課題です。本プロジェクトではVavrを使用して関数型アプローチでnullを回避していますが、以下の状況では注釈ベースのnullability管理が必要です：

1. 外部ライブラリとのインターフェース
2. フレームワーク（Dagger、lsp4j等）との統合部分
3. パフォーマンスクリティカルな箇所でVavrのオーバーヘッドを避けたい場合
4. 既存のJavaエコシステムとの互換性

nullability注釈の選択肢：
- JSR-305（`@javax.annotation.Nullable`等）
- JetBrains annotations
- Eclipse annotations
- JSpecify

## 決定

**JSpecify**を採用し、プロジェクト全体をデフォルトnon-nullとして扱います。

### 実装方針

1. パッケージレベルで`@NullMarked`を適用
2. nullを許容する箇所のみ`@Nullable`を明示
3. NullAwayをOnlyNullMarkedモードで使用

## 根拠

### 1. モダンな標準仕様
- 2024年8月にv1.0.0リリース
- Google、JetBrains、Oracle等による業界標準
- 活発なメンテナンスと将来性

### 2. デフォルトnon-nullの設計思想

パッケージレベルで@NullMarkedを宣言することで、デフォルトで全ての型がnon-nullとなります。nullを許容する場合のみ@Nullableを明示的に付与します。

### 3. NullAwayとの優れた統合

NullAwayの設定で`@NullMarked`スコープのみをチェック対象とすることで、段階的な導入が可能であり、高速な静的解析を実現します。

### 4. Vavrとの共存

内部実装ではVavrの関数型アプローチを使用し、外部APIとのインターフェースではJSpecifyのnullabilityアノテーションを使用します。これにより両方の利点を活かした設計が可能となります。

## 実装ガイドライン

### 1. パッケージ構成
各パッケージに`package-info.java`を作成：
```java
@NullMarked
package com.groovylsp.domain.model;

import org.jspecify.annotations.NullMarked;
```

### 2. 型パラメータ
```java
// nullableな型パラメータを許可
public class Container<T extends @Nullable Object> {
    private @Nullable T value;
}

// non-nullな型パラメータ（デフォルト）
public class SafeContainer<T> {
    private T value;  // Tはnon-null
}
```

### 3. 配列とコレクション
```java
// nullableな要素を持つリスト
List<@Nullable String> nullableStrings;

// nullable配列（配列自体がnull可能）
String @Nullable [] nullableArray;

// nullableな要素を持つ配列
@Nullable String[] arrayOfNullables;
```

### 4. Vavrとの使い分け

| ケース | 推奨アプローチ | 理由 |
|--------|--------------|------|
| ビジネスロジック | Vavr（Either/Option） | エラーハンドリングの明確化 |
| 外部API境界 | JSpecify | 互換性とパフォーマンス |
| DTOクラス | JSpecify | シンプルさ |
| ドメインモデル | 両方を適切に使用 | 文脈に応じて |

## 影響

### ポジティブな影響
- NPEの大幅な削減
- IDEサポートの向上（IntelliJ IDEA等）
- コードの意図が明確に
- 段階的導入が可能
- Kotlinとの相互運用性向上

### ネガティブな影響
- 新しい依存関係の追加
- 学習コスト（ただし最小限）
- 注釈の配置に関する初期の混乱可能性

### 軽減策
- チーム勉強会でJSpecifyの使い方を共有
- コードレビューでの注釈配置チェック
- 自動フォーマッターでの統一

## 代替案

### JSR-305
歴史的に広く使われていましたが、以下の理由で却下：
- 2012年以降メンテナンスされていない
- Javaモジュールシステムとの非互換性
- 不完全な仕様

### JetBrains annotations
IntelliJ IDEA固有で、ツール中立性に欠けるため却下。

## 移行計画

1. **Phase 1**: 新規パッケージから`@NullMarked`適用
2. **Phase 2**: 既存コードの段階的移行（`@NullUnmarked`使用）
3. **Phase 3**: 全体の移行完了とレビュー

## 参考資料

- [JSpecify公式サイト](https://jspecify.dev/)
- [JSpecify User Guide](https://github.com/jspecify/jspecify/wiki/User-Guide)
- [NullAway with JSpecify](https://github.com/uber/NullAway/wiki/JSpecify-Support)