package com.groovylsp.domain.model;

import io.vavr.control.Option;
import java.util.List;
import java.util.Map;


/**
 * JavaDoc/GroovyDocドキュメント情報
 *
 * <p>シンボルに関連付けられたドキュメント情報を保持します。
 */
public record Documentation(
    String summary,
    Option<String> description,
    List<ParamDoc> params,
    Option<String> returns,
    List<ThrowsDoc> exceptions,
    Map<String, String> tags) {

  /** パラメータのドキュメント */
  public record ParamDoc(String name, String description) {}

  /** 例外のドキュメント */
  public record ThrowsDoc(String exceptionType, String description) {}

  /** 空のドキュメントを作成 */
  public static Documentation empty() {
    return new Documentation(
        "", Option.none(), List.of(), Option.none(), List.of(), Map.of());
  }

  /** サマリーのみのドキュメントを作成 */
  public static Documentation withSummary(String summary) {
    return new Documentation(
        summary, Option.none(), List.of(), Option.none(), List.of(), Map.of());
  }

  /** ドキュメントが空かどうか判定 */
  public boolean isEmpty() {
    return summary.isBlank()
        && description.isEmpty()
        && params.isEmpty()
        && returns.isEmpty()
        && exceptions.isEmpty()
        && tags.isEmpty();
  }
}