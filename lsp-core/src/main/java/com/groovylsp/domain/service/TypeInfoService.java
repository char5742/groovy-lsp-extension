package com.groovylsp.domain.service;

import io.vavr.control.Either;
import org.eclipse.lsp4j.Position;
import org.jspecify.annotations.Nullable;

/**
 * 型情報抽出サービスのインターフェース
 *
 * <p>指定された位置にある変数や式の型情報を抽出します。 ホバー機能で使用され、変数の型情報を提供します。
 */
public interface TypeInfoService {

  /**
   * 指定された位置の型情報を取得
   *
   * @param uri ドキュメントのURI
   * @param content ドキュメントの内容
   * @param position 位置情報
   * @return 型情報、またはエラー
   */
  Either<String, TypeInfo> getTypeInfoAt(String uri, String content, Position position);

  /** 型情報 */
  record TypeInfo(
      String name,
      String type,
      Kind kind,
      @Nullable String documentation,
      @Nullable String modifiers) {

    /** 型情報の種類 */
    public enum Kind {
      LOCAL_VARIABLE, // ローカル変数
      FIELD, // フィールド
      PARAMETER, // パラメータ
      METHOD, // メソッド
      CLASS, // クラス
      ENUM, // 列挙型
      ENUM_CONSTANT // 列挙定数
    }
  }
}
