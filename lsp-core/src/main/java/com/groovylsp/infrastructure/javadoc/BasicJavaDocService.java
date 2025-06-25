package com.groovylsp.infrastructure.javadoc;

import com.groovylsp.domain.service.JavaDocService;
import io.vavr.control.Option;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基本的なJDKクラスのJavaDocドキュメントを提供するサービス実装
 *
 * <p>よく使用されるJavaクラス（String、List、Map等）の基本的なドキュメント情報を 内蔵データベースとして提供します。
 */
@Singleton
public class BasicJavaDocService implements JavaDocService {

  private static final Logger logger = LoggerFactory.getLogger(BasicJavaDocService.class);

  private final Map<String, JavaDocInfo> classDocumentation;
  private final Map<String, JavaDocInfo> methodDocumentation;
  private final Map<String, JavaDocInfo> fieldDocumentation;

  @Inject
  public BasicJavaDocService() {
    this.classDocumentation = new HashMap<>();
    this.methodDocumentation = new HashMap<>();
    this.fieldDocumentation = new HashMap<>();

    initializeBuiltinDocumentation();
  }

  /** 組み込みドキュメント情報を初期化 */
  private void initializeBuiltinDocumentation() {
    // java.lang.String
    classDocumentation.put(
        "java.lang.String",
        new JavaDocInfo(
            "Stringクラスは、文字列を表します。",
            "文字列はUnicode文字の列で、不変（immutable）です。"
                + "一度作成されたStringオブジェクトは変更できません。\n\n"
                + "文字列の比較には equals() メソッドを使用してください。"
                + "== 演算子は参照の比較を行うため、文字列の内容比較には適していません。",
            null,
            null,
            null,
            "1.0",
            null,
            null));

    // java.lang.String.length()
    methodDocumentation.put(
        "java.lang.String.length",
        new JavaDocInfo(
            "この文字列の長さを返します。",
            "長さは文字列内のUnicodeコードユニットの数と等しくなります。",
            null,
            "この文字列の長さ",
            null,
            "1.0",
            null,
            null));

    // java.lang.String.substring(int)
    methodDocumentation.put(
        "java.lang.String.substring(int)",
        new JavaDocInfo(
            "この文字列の部分文字列である新しい文字列を返します。",
            "部分文字列は指定されたインデックスの文字から始まり、この文字列の最後まで続きます。",
            "- `beginIndex`: 部分文字列の開始インデックス",
            "指定された部分文字列",
            "- `IndexOutOfBoundsException`: beginIndex が負の値またはこの String オブジェクトの長さより大きい場合",
            "1.0",
            null,
            "`substring(int, int)`"));

    // java.util.List
    classDocumentation.put(
        "java.util.List",
        new JavaDocInfo(
            "順序付けられたコレクション（シーケンスとも呼ばれる）です。",
            "このインターフェースのユーザーは、リスト内の要素が挿入される位置を正確に制御できます。"
                + "ユーザーは、整数のインデックス（リスト内の位置）によって要素にアクセスし、"
                + "リスト内の要素を検索できます。\n\n"
                + "一般的な実装には ArrayList、LinkedList、Vector があります。",
            null,
            null,
            null,
            "1.2",
            null,
            "`Collection`, `ArrayList`, `LinkedList`"));

    // java.util.List.add(E)
    methodDocumentation.put(
        "java.util.List.add(E)",
        new JavaDocInfo(
            "指定された要素をこのリストの最後に追加します。",
            "この操作をサポートしないリストは UnsupportedOperationException をスローします。",
            "- `e`: このリストに追加される要素",
            "このコレクションがこの呼び出しの結果として変更された場合は true",
            "- `UnsupportedOperationException`: この操作がこのリストでサポートされていない場合\n"
                + "- `ClassCastException`: 指定された要素のクラスが原因で、このリストに追加できない場合\n"
                + "- `NullPointerException`: 指定された要素が null で、このリストが null 要素を許可しない場合",
            "1.2",
            null,
            "`add(int, E)`, `remove(Object)`"));

    // java.util.Map
    classDocumentation.put(
        "java.util.Map",
        new JavaDocInfo(
            "キーを値にマップするオブジェクトです。",
            "マップには重複するキーを含めることはできません。各キーは最大1つの値にマップできます。\n\n"
                + "このインターフェースは、辞書または連想配列の数学的概念を置き換えます。"
                + "一般的な実装には HashMap、TreeMap、LinkedHashMap があります。",
            null,
            null,
            null,
            "1.2",
            null,
            "`HashMap`, `TreeMap`, `LinkedHashMap`"));

    // java.util.Map.get(Object)
    methodDocumentation.put(
        "java.util.Map.get(Object)",
        new JavaDocInfo(
            "指定されたキーがマップされている値を返します。",
            "このマップに指定されたキーのマッピングが含まれていない場合は null を返します。"
                + "戻り値の null は、必ずしもマップにキーのマッピングが含まれていないことを示すものではありません。"
                + "マップが明示的にキーを null にマップしている可能性もあります。",
            "- `key`: 関連付けられている値が返されるキー",
            "指定されたキーがマップされている値。このマップにそのキーのマッピングが含まれていない場合は null",
            "- `ClassCastException`: キーがこのマップに適さない型の場合\n"
                + "- `NullPointerException`: 指定されたキーが null で、このマップが null キーを許可しない場合",
            "1.2",
            null,
            "`put(Object, Object)`, `containsKey(Object)`"));

    // java.lang.Object
    classDocumentation.put(
        "java.lang.Object",
        new JavaDocInfo(
            "すべてのJavaクラスの階層のルートクラスです。",
            "すべてのクラスは Object を直接または間接的にスーパークラスとして持ちます。" + "すべてのオブジェクト（配列を含む）は、このクラスのメソッドを実装しています。",
            null,
            null,
            null,
            "1.0",
            null,
            null));

    // java.lang.Object.equals(Object)
    methodDocumentation.put(
        "java.lang.Object.equals(Object)",
        new JavaDocInfo(
            "指定されたオブジェクトとこのオブジェクトが等しいかどうかを示します。",
            "equals メソッドは、null以外のオブジェクト参照での同値関係を実装します：\n"
                + "- 反射的：null以外の参照値 x に対して、x.equals(x) は true を返す\n"
                + "- 対称的：null以外の参照値 x と y に対して、x.equals(y) が true を返す場合のみ、y.equals(x) が true を返す\n"
                + "- 推移的：null以外の参照値 x、y、z に対して、x.equals(y) が true で y.equals(z) が true を返す場合、x.equals(z) は true を返す",
            "- `obj`: 参照オブジェクト",
            "このオブジェクトが obj 引数と同じである場合は true、そうでない場合は false",
            null,
            "1.0",
            null,
            "`hashCode()`, `getClass()`"));

    // java.lang.Integer
    classDocumentation.put(
        "java.lang.Integer",
        new JavaDocInfo(
            "Integer クラスは、プリミティブ型 int の値をオブジェクトにラップします。",
            "Integer 型のオブジェクトには、型が int の単一フィールドが含まれています。\n\n"
                + "さらに、このクラスは int を String に、String を int に変換するいくつかのメソッドと、"
                + "int を処理するときに役立つその他の定数およびメソッドを提供します。",
            null,
            null,
            null,
            "1.0",
            null,
            "`Number`, `Long`, `Double`"));

    logger.info(
        "組み込みJavaDocドキュメント情報を初期化しました（{}クラス、{}メソッド）",
        classDocumentation.size(),
        methodDocumentation.size());
  }

  @Override
  public Option<JavaDocInfo> getClassDocumentation(String className) {
    if (className == null || className.trim().isEmpty()) {
      return Option.none();
    }

    logger.debug("クラスドキュメントを検索: {}", className);

    JavaDocInfo info = classDocumentation.get(className);
    if (info != null) {
      logger.debug("組み込みドキュメントが見つかりました: {}", className);
      return Option.some(info);
    }

    // 配列型の処理（例: String[] -> String）
    if (className.endsWith("[]")) {
      String baseClass = className.substring(0, className.length() - 2);
      Option<JavaDocInfo> baseInfo = getClassDocumentation(baseClass);
      if (baseInfo.isDefined()) {
        var arrayInfo =
            new JavaDocInfo(
                baseInfo.get().summary() + "の配列",
                baseInfo.get().description() + "\n\nこれは配列型です。",
                null,
                null,
                null,
                baseInfo.get().since(),
                baseInfo.get().deprecated(),
                baseInfo.get().see());
        return Option.some(arrayInfo);
      }
    }

    // ジェネリック型の処理（例: List<String> -> List）
    if (className.contains("<")) {
      String baseClass = className.substring(0, className.indexOf('<'));
      return getClassDocumentation(baseClass);
    }

    logger.debug("ドキュメントが見つかりませんでした: {}", className);
    return Option.none();
  }

  @Override
  public Option<JavaDocInfo> getMethodDocumentation(
      String className, String methodName, String[] parameterTypes) {
    if (className == null || methodName == null) {
      return Option.none();
    }

    logger.debug(
        "メソッドドキュメントを検索: {}.{}({})",
        className,
        methodName,
        parameterTypes != null ? String.join(", ", parameterTypes) : "");

    // シンプルな形式でキーを生成
    String simpleKey = className + "." + methodName;
    JavaDocInfo info = methodDocumentation.get(simpleKey);
    if (info != null) {
      logger.debug("組み込みメソッドドキュメントが見つかりました: {}", simpleKey);
      return Option.some(info);
    }

    // パラメータ型付きのキーを試す
    if (parameterTypes != null && parameterTypes.length > 0) {
      String paramKey = simpleKey + "(" + String.join(", ", parameterTypes) + ")";
      info = methodDocumentation.get(paramKey);
      if (info != null) {
        logger.debug("パラメータ付きメソッドドキュメントが見つかりました: {}", paramKey);
        return Option.some(info);
      }

      // 単一パラメータの場合の簡略形式も試す
      if (parameterTypes.length == 1) {
        String singleParamKey = simpleKey + "(" + parameterTypes[0] + ")";
        info = methodDocumentation.get(singleParamKey);
        if (info != null) {
          logger.debug("単一パラメータメソッドドキュメントが見つかりました: {}", singleParamKey);
          return Option.some(info);
        }
      }
    }

    logger.debug("メソッドドキュメントが見つかりませんでした: {}.{}", className, methodName);
    return Option.none();
  }

  @Override
  public Option<JavaDocInfo> getFieldDocumentation(String className, String fieldName) {
    if (className == null || fieldName == null) {
      return Option.none();
    }

    logger.debug("フィールドドキュメントを検索: {}.{}", className, fieldName);

    String key = className + "." + fieldName;
    JavaDocInfo info = fieldDocumentation.get(key);
    if (info != null) {
      logger.debug("組み込みフィールドドキュメントが見つかりました: {}", key);
      return Option.some(info);
    }

    logger.debug("フィールドドキュメントが見つかりませんでした: {}.{}", className, fieldName);
    return Option.none();
  }
}
