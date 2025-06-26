package com.groovylsp.infrastructure.ast;

import com.groovylsp.domain.model.Symbol;
import com.groovylsp.domain.service.SymbolExtractionService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GroovyのASTからシンボル情報を抽出するサービスの実装
 *
 * <p>GroovyAstParserを使用してソースコードを解析し、 クラス、メソッド、フィールド、プロパティなどのシンボル情報を抽出します。
 */
@Singleton
public class GroovySymbolExtractionService implements SymbolExtractionService {

  private static final Logger logger = LoggerFactory.getLogger(GroovySymbolExtractionService.class);

  /** シンボル名の推定文字数のデフォルト値 */
  private static final int DEFAULT_SYMBOL_NAME_LENGTH = 10;

  private final GroovyAstParser parser;

  /** クラス名解決結果のキャッシュ（パフォーマンス改善のため） */
  private final Map<String, Boolean> classExistenceCache = createLRUCache(1000);

  @Inject
  public GroovySymbolExtractionService(GroovyAstParser parser) {
    this.parser = parser;
  }

  /**
   * LRUキャッシュを作成する
   *
   * @param maxSize 最大サイズ
   * @return LRUキャッシュ
   */
  private static Map<String, Boolean> createLRUCache(int maxSize) {
    return new LinkedHashMap<String, Boolean>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
        return size() > maxSize;
      }
    };
  }

  /** キャッシュをクリアする（メモリ解放用） */
  public void clearCache() {
    synchronized (classExistenceCache) {
      classExistenceCache.clear();
    }
  }

  @Override
  public Either<String, List<Symbol>> extractSymbols(String uri, String content) {
    logger.debug("シンボル抽出を開始: {}", uri);

    // ファイル名を抽出（URIから）
    String fileName = extractFileName(uri);

    return parser
        .parse(fileName, content)
        .mapLeft(error -> "パースエラー: " + error.message())
        .map(
            parseResult -> {
              List<Symbol> symbols = new ArrayList<>();
              ModuleNode moduleNode = parseResult.moduleNode();

              if (moduleNode != null) {
                // すべてのクラスを処理
                for (ClassNode classNode : parseResult.getClasses()) {
                  if (shouldIncludeClass(classNode)) {
                    Symbol classSymbol = extractClassSymbol(classNode, moduleNode);
                    symbols.add(classSymbol);
                  }
                }
              }

              return symbols;
            });
  }

  /**
   * URIからファイル名を抽出
   *
   * @param uri ファイルのURI
   * @return ファイル名
   */
  private String extractFileName(String uri) {
    if (uri.startsWith("file://")) {
      uri = uri.substring(7);
    }
    int lastSlash = uri.lastIndexOf('/');
    return lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
  }

  /**
   * クラスを含めるべきかどうかを判定
   *
   * @param classNode クラスノード
   * @return 含めるべきならtrue
   */
  private boolean shouldIncludeClass(ClassNode classNode) {
    // スクリプトクラス（ファイル名がクラス名になっているもの）は除外
    String className = classNode.getName();
    return !className.contains("$") && !classNode.isScript();
  }

  /**
   * クラスノードからシンボル情報を抽出
   *
   * @param classNode クラスノード
   * @param moduleNode モジュールノード
   * @return クラスのシンボル情報
   */
  private Symbol extractClassSymbol(ClassNode classNode, ModuleNode moduleNode) {
    List<Symbol> children = new ArrayList<>();

    // フィールドを抽出
    for (FieldNode field : classNode.getFields()) {
      if (!field.isSynthetic() && !field.getName().startsWith("$")) {
        children.add(extractFieldSymbol(field, moduleNode));
      }
    }

    // プロパティを抽出
    for (PropertyNode property : classNode.getProperties()) {
      if (!property.isSynthetic()) {
        children.add(extractPropertySymbol(property, moduleNode));
      }
    }

    // メソッドを抽出
    for (MethodNode method : classNode.getMethods()) {
      if (shouldIncludeMethod(method)) {
        children.add(extractMethodSymbol(method));
      }
    }

    // クラスの範囲を計算
    Range classRange =
        createRange(
            classNode.getLineNumber(),
            classNode.getColumnNumber(),
            classNode.getLastLineNumber(),
            classNode.getLastColumnNumber());

    // 選択範囲（クラス名の部分）
    // selectionRangeがclassRangeに確実に含まれるようにする
    // クラス名の開始位置は通常columnNumberから少し後ろにある
    int nameStartColumn = classNode.getColumnNumber();
    int nameEndColumn =
        Math.min(nameStartColumn + DEFAULT_SYMBOL_NAME_LENGTH, classNode.getLastColumnNumber());

    // selectionRangeをclassRangeの内側に確実に収める
    Range selectionRange =
        createSafeSelectionRange(
            classNode.getLineNumber(),
            nameStartColumn,
            classNode.getLineNumber(),
            nameEndColumn,
            classRange);

    // 詳細情報（継承情報など）
    String detail = createClassDetail(classNode);

    return Symbol.createWithChildren(
        classNode.getName(),
        classNode.isInterface() ? SymbolKind.Interface : SymbolKind.Class,
        classRange,
        selectionRange,
        detail,
        children);
  }

  /**
   * メソッドを含めるべきかどうかを判定
   *
   * @param method メソッドノード
   * @return 含めるべきならtrue
   */
  private boolean shouldIncludeMethod(MethodNode method) {
    // 自動生成されたメソッドや特殊なメソッドを除外
    String methodName = method.getName();
    return !method.isSynthetic()
        && !methodName.startsWith("$")
        && !methodName.equals("<clinit>"); // <init>（コンストラクタ）は含める
  }

  /**
   * フィールドノードからシンボル情報を抽出
   *
   * @param field フィールドノード
   * @param moduleNode モジュールノード
   * @return フィールドのシンボル情報
   */
  private Symbol extractFieldSymbol(FieldNode field, ModuleNode moduleNode) {
    Range range =
        createRange(
            field.getLineNumber(),
            field.getColumnNumber(),
            field.getLastLineNumber(),
            field.getLastColumnNumber());

    // selectionRangeがrangeに確実に含まれるようにする
    int nameEndColumn =
        Math.min(field.getColumnNumber() + DEFAULT_SYMBOL_NAME_LENGTH, field.getLastColumnNumber());
    Range selectionRange =
        createSafeSelectionRange(
            field.getLineNumber(),
            field.getColumnNumber(),
            field.getLineNumber(),
            nameEndColumn,
            range);

    // フィールドの型を推論
    ClassNode fieldType = inferFieldType(field, moduleNode);
    String detail = formatTypeDetail(fieldType);

    return Symbol.create(field.getName(), SymbolKind.Field, range, selectionRange, detail);
  }

  /**
   * プロパティノードからシンボル情報を抽出
   *
   * @param property プロパティノード
   * @param moduleNode モジュールノード
   * @return プロパティのシンボル情報
   */
  private Symbol extractPropertySymbol(PropertyNode property, ModuleNode moduleNode) {
    Range range =
        createRange(
            property.getLineNumber(),
            property.getColumnNumber(),
            property.getLineNumber(),
            property.getLastColumnNumber());

    // selectionRangeがrangeに確実に含まれるようにする
    int nameEndColumn =
        Math.min(
            property.getColumnNumber() + DEFAULT_SYMBOL_NAME_LENGTH,
            property.getLastColumnNumber());
    Range selectionRange =
        createSafeSelectionRange(
            property.getLineNumber(),
            property.getColumnNumber(),
            property.getLineNumber(),
            nameEndColumn,
            range);

    // プロパティの基になるフィールドから型を推論
    ClassNode propertyType;
    FieldNode field = property.getField();
    if (field != null) {
      propertyType = inferFieldType(field, moduleNode);
    } else {
      propertyType = property.getType();
    }
    String detail = formatTypeDetail(propertyType);

    return Symbol.create(property.getName(), SymbolKind.Property, range, selectionRange, detail);
  }

  /**
   * メソッドノードからシンボル情報を抽出
   *
   * @param method メソッドノード
   * @return メソッドのシンボル情報
   */
  private Symbol extractMethodSymbol(MethodNode method) {
    Range range =
        createRange(
            method.getLineNumber(),
            method.getColumnNumber(),
            method.getLastLineNumber(),
            method.getLastColumnNumber());

    // selectionRangeがrangeに確実に含まれるようにする
    int nameEndColumn =
        Math.min(
            method.getColumnNumber() + DEFAULT_SYMBOL_NAME_LENGTH, method.getLastColumnNumber());
    Range selectionRange =
        createSafeSelectionRange(
            method.getLineNumber(),
            method.getColumnNumber(),
            method.getLineNumber(),
            nameEndColumn,
            range);

    // メソッドシグネチャを作成
    String detail = createMethodSignature(method);

    return Symbol.create(method.getName(), SymbolKind.Method, range, selectionRange, detail);
  }

  /**
   * クラスの詳細情報を作成
   *
   * @param classNode クラスノード
   * @return 詳細情報文字列
   */
  private String createClassDetail(ClassNode classNode) {
    var detail = new StringBuilder();

    // 継承クラス
    ClassNode superClass = classNode.getSuperClass();
    if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
      detail.append("extends ").append(superClass.getNameWithoutPackage());
    }

    // 実装インターフェース
    var interfaces = classNode.getInterfaces();
    if (interfaces != null && interfaces.length > 0) {
      if (detail.length() > 0) {
        detail.append(" ");
      }
      detail.append("implements ");
      for (int i = 0; i < interfaces.length; i++) {
        if (i > 0) {
          detail.append(", ");
        }
        detail.append(interfaces[i].getNameWithoutPackage());
      }
    }

    // 詳細情報がある場合は先頭に ": " を追加して統一形式に
    if (detail.length() > 0) {
      return ": " + detail.toString();
    }

    return "";
  }

  /**
   * メソッドシグネチャを作成
   *
   * @param method メソッドノード
   * @return シグネチャ文字列
   */
  private String createMethodSignature(MethodNode method) {
    var signature = new StringBuilder();
    signature.append("(");

    var parameters = method.getParameters();
    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        if (i > 0) {
          signature.append(", ");
        }
        signature.append(parameters[i].getType().getNameWithoutPackage());
        signature.append(" ");
        signature.append(parameters[i].getName());
      }
    }

    signature.append(")");

    // 戻り値の型
    signature.append(": ");
    signature.append(method.getReturnType().getNameWithoutPackage());

    return signature.toString();
  }

  /**
   * 安全なselectionRangeを作成（必ずfullRangeに含まれることを保証）
   *
   * @param startLine 開始行（1ベース）
   * @param startColumn 開始列（1ベース）
   * @param endLine 終了行（1ベース）
   * @param endColumn 終了列（1ベース）
   * @param fullRange 含まれるべき全体範囲
   * @return 安全なselectionRange
   */
  private Range createSafeSelectionRange(
      int startLine, int startColumn, int endLine, int endColumn, Range fullRange) {
    // 通常の範囲を作成
    Range selectionRange = createRange(startLine, startColumn, endLine, endColumn);

    // fullRangeに含まれるように調整
    Position selStart = selectionRange.getStart();
    Position selEnd = selectionRange.getEnd();
    Position fullStart = fullRange.getStart();
    Position fullEnd = fullRange.getEnd();

    // 開始位置の調整
    if (selStart.getLine() < fullStart.getLine()
        || (selStart.getLine() == fullStart.getLine()
            && selStart.getCharacter() < fullStart.getCharacter())) {
      selStart = fullStart;
    }

    // 終了位置の調整
    if (selEnd.getLine() > fullEnd.getLine()
        || (selEnd.getLine() == fullEnd.getLine()
            && selEnd.getCharacter() > fullEnd.getCharacter())) {
      selEnd = fullEnd;
    }

    return new Range(selStart, selEnd);
  }

  /**
   * 型情報の詳細を統一形式でフォーマット
   *
   * @param type クラスノード
   * @return フォーマットされた型情報
   */
  private String formatTypeDetail(ClassNode type) {
    if (type == null) {
      return "";
    }
    String typeName = type.getNameWithoutPackage();
    // ジェネリクス対応の場合は将来ここで処理
    return ": " + typeName;
  }

  /**
   * フィールドの型を推論
   *
   * @param field フィールドノード
   * @param moduleNode モジュールノード
   * @return 推論された型（推論できない場合は宣言された型）
   */
  private ClassNode inferFieldType(FieldNode field, ModuleNode moduleNode) {
    // 明示的に型が宣言されている場合（defやObjectでない場合）はその型を使用
    ClassNode declaredType = field.getType();
    if (!field.isDynamicTyped() && !"java.lang.Object".equals(declaredType.getName())) {
      return declaredType;
    }

    // 初期化式から型を推論
    if (field.hasInitialExpression()) {
      Expression init = field.getInitialExpression();
      ClassNode inferredType = inferTypeFromExpression(init, moduleNode);
      if (inferredType != null && !"java.lang.Object".equals(inferredType.getName())) {
        return inferredType;
      }
    }

    // 推論できない場合は宣言された型を返す
    return declaredType;
  }

  /**
   * 式から型を推論
   *
   * @param expression 式
   * @param moduleNode モジュールノード
   * @return 推論された型（推論できない場合はnull）
   */
  private ClassNode inferTypeFromExpression(Expression expression, ModuleNode moduleNode) {
    if (expression instanceof ConstructorCallExpression) {
      // new User(...) の場合、User型を返す
      var ctorCall = (ConstructorCallExpression) expression;
      return ctorCall.getType();
    } else if (expression instanceof ConstantExpression) {
      // リテラル値から型を推論
      var constExpr = (ConstantExpression) expression;
      Object value = constExpr.getValue();
      if (value instanceof String) {
        return ClassHelper.STRING_TYPE;
      } else if (value instanceof Integer) {
        return ClassHelper.int_TYPE;
      } else if (value instanceof Long) {
        return ClassHelper.long_TYPE;
      } else if (value instanceof Boolean) {
        return ClassHelper.boolean_TYPE;
      } else if (value instanceof Double) {
        return ClassHelper.double_TYPE;
      } else if (value instanceof Float) {
        return ClassHelper.float_TYPE;
      } else if (value instanceof java.math.BigDecimal) {
        return ClassHelper.make(java.math.BigDecimal.class);
      }
    } else if (expression instanceof ListExpression) {
      // リストリテラル [] はArrayListとして推論
      return ClassHelper.make(java.util.ArrayList.class);
    } else if (expression instanceof MapExpression) {
      // マップリテラル [:] はLinkedHashMapとして推論
      return ClassHelper.make(java.util.LinkedHashMap.class);
    } else if (expression instanceof MethodCallExpression) {
      // SpockのMock/Stub/Spyの処理
      var methodCall = (MethodCallExpression) expression;
      String methodName = methodCall.getMethodAsString();

      if ("Mock".equals(methodName) || "Stub".equals(methodName) || "Spy".equals(methodName)) {
        // Mock(UserService) の引数から型を取得
        Expression arguments = methodCall.getArguments();
        if (arguments instanceof ArgumentListExpression) {
          var argList = (ArgumentListExpression) arguments;
          if (!argList.getExpressions().isEmpty()) {
            Expression firstArg = argList.getExpression(0);
            if (firstArg instanceof ClassExpression) {
              var classExpr = (ClassExpression) firstArg;
              return classExpr.getType();
            } else if (firstArg instanceof org.codehaus.groovy.ast.expr.VariableExpression) {
              // Mock(UserService)のように、引数が変数として扱われる場合
              var varExpr = (org.codehaus.groovy.ast.expr.VariableExpression) firstArg;
              String typeName = varExpr.getName();
              // 変数名から型を解決
              ClassNode resolvedType = resolveClassName(typeName, moduleNode);
              if (resolvedType != null) {
                return resolvedType;
              }
              // 解決できない場合は型名から直接ClassNodeを作成（フォールバック）
              return ClassHelper.make(typeName);
            }
          }
        }
      }
    }

    // その他の場合は式自体の型を返す
    return expression.getType();
  }

  /**
   * 範囲を作成（Groovyの行番号は1ベース、LSPは0ベース）
   *
   * @param startLine 開始行（1ベース）
   * @param startColumn 開始列（1ベース）
   * @param endLine 終了行（1ベース）
   * @param endColumn 終了列（1ベース）
   * @return LSPのRange
   */
  private Range createRange(int startLine, int startColumn, int endLine, int endColumn) {
    // Groovyの行番号とカラム番号は1ベース、LSPは0ベースなので変換
    // 負の値にならないように保護
    int lspStartLine = Math.max(0, startLine - 1);
    int lspStartColumn = Math.max(0, startColumn - 1);
    int lspEndLine = Math.max(0, endLine - 1);
    int lspEndColumn = Math.max(0, endColumn - 1);

    // 開始位置が終了位置より後にならないように保護
    if (lspEndLine < lspStartLine
        || (lspEndLine == lspStartLine && lspEndColumn < lspStartColumn)) {
      lspEndLine = lspStartLine;
      lspEndColumn = lspStartColumn;
    }

    return new Range(
        new Position(lspStartLine, lspStartColumn), new Position(lspEndLine, lspEndColumn));
  }

  /**
   * クラス名を解決（インポートを考慮）
   *
   * @param className クラス名（単純名または完全修飾名）
   * @param moduleNode モジュールノード
   * @return 解決されたClassNode（解決できない場合はnull）
   */
  private @Nullable ClassNode resolveClassName(String className, ModuleNode moduleNode) {
    if (className == null || moduleNode == null) {
      return null;
    }

    // 完全修飾名の場合はそのまま作成
    if (className.contains(".")) {
      return ClassHelper.make(className);
    }

    // 優先順位1: 明示的なインポート文から解決を試みる
    for (var importNode : moduleNode.getImports()) {
      String importClassName = importNode.getClassName();
      if (importClassName.endsWith("." + className)) {
        return ClassHelper.make(importClassName);
      }
    }

    // 優先順位2: 同一パッケージのクラス（将来の拡張用）
    String packageName = moduleNode.getPackageName();
    if (packageName != null && !packageName.isEmpty()) {
      String samePackageClass = packageName + "." + className;
      if (isClassExists(samePackageClass)) {
        return ClassHelper.make(samePackageClass);
      }
    }

    // 優先順位3: スターインポートから解決を試みる
    for (var starImport : moduleNode.getStarImports()) {
      String starPackageName = starImport.getPackageName();
      if (starPackageName != null) {
        // パッケージ名が "." で終わる場合は削除
        if (starPackageName.endsWith(".")) {
          starPackageName = starPackageName.substring(0, starPackageName.length() - 1);
        }
        String fullClassName = starPackageName + "." + className;
        if (isClassExists(fullClassName)) {
          return ClassHelper.make(fullClassName);
        }
      }
    }

    // 優先順位4: デフォルトインポート（java.lang.*、groovy.lang.*など）
    String[] defaultPackages = {
      "java.lang", "groovy.lang", "java.util", "java.io", "java.net", "groovy.util"
    };
    for (String pkg : defaultPackages) {
      String fullClassName = pkg + "." + className;
      if (isClassExists(fullClassName)) {
        return ClassHelper.make(fullClassName);
      }
    }

    return null;
  }

  /**
   * クラスが存在するかどうかをキャッシュ付きで確認
   *
   * @param fullClassName 完全修飾クラス名
   * @return クラスが存在する場合true
   */
  private boolean isClassExists(String fullClassName) {
    synchronized (classExistenceCache) {
      return classExistenceCache.computeIfAbsent(
          fullClassName,
          className -> {
            try {
              Class.forName(className);
              return true;
            } catch (ClassNotFoundException e) {
              // プロジェクト固有のクラスは解決できないため、デバッグレベルでログ出力
              logger.debug("Class not found in classpath: {}", className);
              return false;
            }
          });
    }
  }
}
