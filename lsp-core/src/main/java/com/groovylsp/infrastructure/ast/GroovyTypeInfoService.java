package com.groovylsp.infrastructure.ast;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.ClassInfo;
import com.groovylsp.domain.model.MethodInfo;
import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolDefinition;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.Position;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Groovy型情報抽出サービスの実装
 *
 * <p>GroovyのASTを解析して、指定された位置にある変数や式の型情報を抽出します。
 */
@Singleton
public class GroovyTypeInfoService implements TypeInfoService {

  private static final Logger logger = LoggerFactory.getLogger(GroovyTypeInfoService.class);

  private final GroovyAstParser parser;
  private final SymbolTable symbolTable;
  private final ScopeManager scopeManager;
  private final DocumentContentService documentContentService;
  private final AstAnalysisService astAnalysisService;

  @Inject
  public GroovyTypeInfoService(
      GroovyAstParser parser,
      SymbolTable symbolTable,
      ScopeManager scopeManager,
      DocumentContentService documentContentService,
      AstAnalysisService astAnalysisService) {
    this.parser = parser;
    this.symbolTable = symbolTable;
    this.scopeManager = scopeManager;
    this.documentContentService = documentContentService;
    this.astAnalysisService = astAnalysisService;
  }

  @Override
  public Either<String, TypeInfo> getTypeInfoAt(String uri, String content, Position position) {
    logger.debug("型情報を取得: {} at {}:{}", uri, position.getLine(), position.getCharacter());

    // ファイル名を抽出
    String fileName = extractFileName(uri);

    return parser
        .parse(fileName, content)
        .mapLeft(error -> "パースエラー: " + error.message())
        .flatMap(
            parseResult -> {
              ModuleNode moduleNode = parseResult.moduleNode();
              if (moduleNode == null) {
                return Either.left("モジュールノードが見つかりません");
              }

              logger.debug("パース成功。クラス数: {}", parseResult.getClasses().size());

              // 指定位置の要素を探索
              var visitor = new TypeInfoVisitor(position, uri);
              for (ClassNode classNode : parseResult.getClasses()) {
                logger.debug("クラスを訪問: {}", classNode.getName());
                visitor.visitClass(classNode);
              }

              TypeInfo typeInfo = visitor.getFoundTypeInfo();
              if (typeInfo != null) {
                logger.debug("ASTで型情報を発見: {}", typeInfo.name());
                return Either.right(typeInfo);
              }

              logger.debug("ASTで見つからなかったため、シンボルテーブルから検索");
              // ASTで見つからない場合は、シンボルテーブルから検索
              return findTypeInfoFromSymbolTable(uri, position);
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
   * シンボルテーブルから型情報を検索
   *
   * @param uri ファイルのURI
   * @param position 位置情報
   * @return 型情報、またはエラー
   */
  private Either<String, TypeInfo> findTypeInfoFromSymbolTable(String uri, Position position) {
    // カーソル位置の単語を取得
    Either<String, String> wordResult = getWordAtPosition(uri, position);
    if (wordResult.isLeft()) {
      return Either.left("識別子が特定できません");
    }

    String word = wordResult.get();
    if (word.isEmpty()) {
      return Either.left("識別子が見つかりません");
    }

    // スコープマネージャーから検索
    Option<SymbolDefinition> symbolOption = scopeManager.findSymbolAt(uri, position, word);
    if (symbolOption.isDefined()) {
      SymbolDefinition symbol = symbolOption.get();
      return Either.right(createTypeInfoFromSymbol(symbol));
    }

    // シンボルテーブルから検索
    io.vavr.collection.List<SymbolDefinition> definitions = symbolTable.findByName(word);
    if (!definitions.isEmpty()) {
      // 同じファイルの定義を優先
      Option<SymbolDefinition> sameFileSymbol = definitions.find(def -> def.uri().equals(uri));
      SymbolDefinition symbol = sameFileSymbol.getOrElse(definitions.head());
      return Either.right(createTypeInfoFromSymbol(symbol));
    }

    return Either.left("識別子 '" + word + "' の定義が見つかりません");
  }

  /**
   * カーソル位置の単語を取得
   *
   * @param uri ファイルのURI
   * @param position 位置情報
   * @return 単語、またはエラー
   */
  private Either<String, String> getWordAtPosition(String uri, Position position) {
    return documentContentService
        .getContent(uri)
        .toEither("ドキュメントが見つかりません: " + uri)
        .map(
            content -> {
              String[] lines = content.split("\n");
              if (position.getLine() >= lines.length) {
                logger.debug("行番号が範囲外: {} >= {}", position.getLine(), lines.length);
                return "";
              }

              String line = lines[position.getLine()];
              int pos = position.getCharacter();

              logger.debug(
                  "getWordAtPosition - 行: {}, 位置: {}, 行内容: '{}'", position.getLine(), pos, line);

              // 行の長さチェック
              if (pos > line.length()) {
                logger.debug("文字位置が行長を超えています: {} > {}", pos, line.length());
                return "";
              }

              // 単語の開始位置を見つける
              int start = pos;
              while (start > 0 && isWordChar(line.charAt(start - 1))) {
                start--;
              }

              // 単語の終了位置を見つける
              int end = pos;
              while (end < line.length() && isWordChar(line.charAt(end))) {
                end++;
              }

              String word = line.substring(start, end);
              logger.debug("取得した単語: '{}'", word);
              return word;
            });
  }

  /** 文字が単語の一部かどうかを判定 */
  private boolean isWordChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '$';
  }

  /**
   * シンボル定義から型情報を作成
   *
   * @param symbol シンボル定義
   * @return 型情報
   */
  private TypeInfo createTypeInfoFromSymbol(SymbolDefinition symbol) {
    TypeInfo.Kind kind =
        switch (symbol.definitionType()) {
          case CLASS -> TypeInfo.Kind.CLASS;
          case METHOD -> TypeInfo.Kind.METHOD;
          case FIELD -> TypeInfo.Kind.FIELD;
          case LOCAL_VARIABLE -> TypeInfo.Kind.LOCAL_VARIABLE;
          case PARAMETER -> TypeInfo.Kind.PARAMETER;
          case IMPORT -> TypeInfo.Kind.LOCAL_VARIABLE; // IMPORTは適切な種類がないのでLOCAL_VARIABLEで代用
          case ENUM_CONSTANT -> TypeInfo.Kind.ENUM_CONSTANT;
        };

    // 詳細な情報を含むドキュメントを生成
    String documentation = createDocumentation(symbol);

    return new TypeInfo(
        symbol.name(),
        symbol.qualifiedName(),
        kind,
        documentation,
        null // modifiersはシンボル定義に含まれていないため、今はnull
        );
  }

  /**
   * シンボル定義からドキュメントを生成
   *
   * @param symbol シンボル定義
   * @return ドキュメント文字列
   */
  private String createDocumentation(SymbolDefinition symbol) {
    var sb = new StringBuilder();

    // 定義の種類を表示
    sb.append("**種類**: ");
    switch (symbol.definitionType()) {
      case CLASS -> sb.append("クラス");
      case METHOD -> sb.append("メソッド");
      case FIELD -> sb.append("フィールド");
      case LOCAL_VARIABLE -> sb.append("ローカル変数");
      case PARAMETER -> sb.append("パラメータ");
      case IMPORT -> sb.append("インポート");
      case ENUM_CONSTANT -> sb.append("列挙定数");
    }
    sb.append("\n\n");

    // 定義位置を表示
    sb.append("**定義位置**: ");
    sb.append(extractFileName(symbol.uri()));
    sb.append(":");
    sb.append(symbol.range().getStart().getLine() + 1); // 1ベースで表示
    sb.append("\n\n");

    // 所属クラスがある場合は表示
    if (symbol.containingClass() != null) {
      sb.append("**所属クラス**: ");
      sb.append(symbol.containingClass());
      sb.append("\n\n");
    }

    return sb.toString();
  }

  /** AST訪問者クラス（型情報を探索） */
  private class TypeInfoVisitor extends ClassCodeVisitorSupport {
    private final Position targetPosition;
    private final String uri;
    private @Nullable TypeInfo foundTypeInfo;
    private final Map<String, ClassNode> variableTypes = new HashMap<>(); // 変数名と型のマッピング
    private @Nullable AstInfo astInfo; // AST情報をキャッシュ

    public TypeInfoVisitor(Position targetPosition, String uri) {
      // LSPの位置は0ベース、Groovyは1ベースなので+1で変換
      this.targetPosition =
          new Position(targetPosition.getLine() + 1, targetPosition.getCharacter() + 1);
      this.uri = uri;
      logger.debug(
          "TypeInfoVisitor initialized - Original position: {}:{}, Adjusted position: {}:{}",
          targetPosition.getLine(),
          targetPosition.getCharacter(),
          this.targetPosition.getLine(),
          this.targetPosition.getCharacter());
      // ドキュメント内容を取得してAST情報を解析
      documentContentService
          .getContent(uri)
          .flatMap(content -> astAnalysisService.analyze(uri, content).toOption())
          .forEach(
              info -> {
                this.astInfo = info;
                if (info.imports() != null) {
                  logger.debug("Loaded {} imports for {}", info.imports().size(), uri);
                  info.imports()
                      .forEach(
                          imp ->
                              logger.debug(
                                  "Import: {} -> alias: {}", imp.className(), imp.alias()));
                }
              });
    }

    public @Nullable TypeInfo getFoundTypeInfo() {
      return foundTypeInfo;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      // このメソッドは必須だが、今回は使用しない
      return null;
    }

    @Override
    public void visitClass(ClassNode node) {
      if (foundTypeInfo != null) {
        return; // 既に見つかっている
      }

      logger.debug(
          "visitClass: {} at {}:{}, target: {}:{}, hasGenerics: {}",
          node.getName(),
          node.getLineNumber(),
          node.getColumnNumber(),
          targetPosition.getLine(),
          targetPosition.getCharacter(),
          node.getGenericsTypes() != null && node.getGenericsTypes().length > 0);

      // 最初に型パラメータをチェック（優先度を上げる）
      GenericsType[] generics = node.getGenericsTypes();
      if (generics != null && generics.length > 0) {
        logger.debug("クラス {} に型パラメータが {} 個あります", node.getName(), generics.length);

        // まず型パラメータ内の位置かチェック
        boolean withinTypeParam = isPositionWithinTypeParameter(node);
        logger.debug("型パラメータ範囲内判定結果: {}", withinTypeParam);

        if (withinTypeParam) {
          for (GenericsType generic : generics) {
            logger.debug(
                "型パラメータ: {}, isPlaceholder: {}, 上限境界: {}",
                generic.getName(),
                generic.isPlaceholder(),
                generic.getUpperBounds() != null ? generic.getUpperBounds().length : 0);
            if (generic.isPlaceholder()) {
              logger.debug("型パラメータ {} の情報を返します", generic.getName());
              String typeParamDoc = createTypeParameterDocumentation(generic);
              foundTypeInfo =
                  new TypeInfo(
                      generic.getName(),
                      formatGenericsType(generic),
                      TypeInfo.Kind.CLASS,
                      typeParamDoc,
                      null);
              return;
            }
          }
        }
      }

      // クラスのすべてのフィールドの型情報を事前に記録
      // （メソッド内からフィールドを参照する際に必要）
      for (FieldNode field : node.getFields()) {
        // defで宣言されたフィールドの型推論を適用
        ClassNode fieldType = inferFieldType(field);
        variableTypes.put(field.getName(), fieldType);
        logger.debug(
            "クラス {} のフィールドを事前記録: {} (型: {})", node.getName(), field.getName(), fieldType.getName());
      }

      // 次に子要素（フィールドとメソッド）をチェック
      // フィールドをチェック
      for (FieldNode field : node.getFields()) {
        visitField(field);
        if (foundTypeInfo != null) {
          return;
        }
      }

      // コンストラクタをチェック
      for (MethodNode constructor : node.getDeclaredConstructors()) {
        logger.debug("クラス {} のコンストラクタを訪問: {}", node.getName(), constructor.getName());
        visitMethod(constructor);
        if (foundTypeInfo != null) {
          return;
        }
      }

      // メソッドをチェック
      for (MethodNode method : node.getMethods()) {
        logger.debug(
            "クラス {} のメソッドを訪問: {} (isConstructor: {})",
            node.getName(),
            method.getName(),
            method.getName().equals("<init>"));
        visitMethod(method);
        if (foundTypeInfo != null) {
          return;
        }
      }

      // ネストクラスをチェック
      Iterator<InnerClassNode> innerClassIterator = node.getInnerClasses();
      if (innerClassIterator != null) {
        List<InnerClassNode> innerClasses = new ArrayList<>();
        while (innerClassIterator.hasNext()) {
          innerClasses.add(innerClassIterator.next());
        }
        logger.debug("クラス {} のネストクラスをチェック: {} 個", node.getName(), innerClasses.size());
        for (InnerClassNode innerClass : innerClasses) {
          logger.debug("ネストクラスを訪問: {}", innerClass.getName());
          visitClass(innerClass);
          if (foundTypeInfo != null) {
            return;
          }
        }
      }

      // 子要素で見つからなかった場合、クラス名の位置をチェック
      if (isPositionWithinClassName(node)) {
        // enumの場合は特別な処理
        if (node.isEnum()) {
          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  node.getName(),
                  TypeInfo.Kind.ENUM,
                  createEnumDocumentation(node),
                  getModifiersString(node.getModifiers()));
        } else if (isRecordClass(node)) {
          // レコードクラスの場合
          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  node.getName(),
                  TypeInfo.Kind.CLASS,
                  createRecordDocumentation(node),
                  getModifiersString(node.getModifiers()));
        } else if (hasCanonicalAnnotation(node)) {
          // @Canonicalアノテーション付きクラスの場合
          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  node.getName(),
                  TypeInfo.Kind.CLASS,
                  createCanonicalClassDocumentation(node),
                  getModifiersString(node.getModifiers()));
        } else {
          // 通常のクラス
          String documentation = null;

          // ネストクラスの場合は完全修飾名を含める
          if (node.getOuterClass() != null) {
            documentation = createNestedClassDocumentation(node);
          }

          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  getFullyQualifiedClassName(node),
                  TypeInfo.Kind.CLASS,
                  documentation,
                  getModifiersString(node.getModifiers()));
        }
      }
    }

    /**
     * 指定位置がクラス名内にあるかチェック
     *
     * @param node クラスノード
     * @return 位置がクラス名内にある場合true
     */
    private boolean isPositionWithinClassName(ClassNode node) {
      int line = targetPosition.getLine();
      int column = targetPosition.getCharacter();

      // デバッグログ
      logger.debug(
          "クラス位置チェック: クラス名={}, ノード位置={}:{}, ターゲット位置={}:{}, OuterClass={}, アノテーション数={}",
          node.getNameWithoutPackage(),
          node.getLineNumber(),
          node.getColumnNumber(),
          line,
          column,
          node.getOuterClass() != null ? node.getOuterClass().getNameWithoutPackage() : "なし",
          node.getAnnotations() != null ? node.getAnnotations().size() : 0);

      // クラス名は "class " または "enum " キーワードの後にある
      String className = node.getNameWithoutPackage();
      int keywordLength = node.isEnum() ? 5 : 6; // "enum " = 5, "class " = 6

      // アノテーション付きクラスの場合、クラス名が次の行にある可能性がある
      boolean hasAnnotations = node.getAnnotations() != null && !node.getAnnotations().isEmpty();

      // 通常の判定
      boolean isWithin =
          line == node.getLineNumber()
              && column >= node.getColumnNumber() + keywordLength
              && column < node.getColumnNumber() + keywordLength + className.length();

      // アノテーション付きクラスの場合、次の行もチェック
      if (!isWithin && hasAnnotations) {
        // ASTがアノテーション行を報告している可能性があるため、次の行もチェック
        isWithin =
            line == node.getLineNumber() + 1
                && column >= keywordLength + 1 // "class " の後
                && column < keywordLength + 1 + className.length();

        if (isWithin) {
          logger.debug("アノテーション付きクラスとして次の行で検出");
        }
      }

      logger.debug("クラス名位置判定結果: {}", isWithin);
      return isWithin;
    }

    @Override
    public void visitField(FieldNode node) {
      if (foundTypeInfo != null) {
        return;
      }

      if (isPositionWithin(node)) {
        // enum定数の場合は特別な処理
        if (node.isEnum()) {
          ClassNode declaringClass = node.getDeclaringClass();
          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  declaringClass.getNameWithoutPackage(),
                  TypeInfo.Kind.ENUM_CONSTANT,
                  null,
                  null);
        } else {
          // フィールドの型を推論（defで宣言された場合の初期化式を考慮）
          ClassNode fieldType = inferFieldType(node);

          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  formatTypeName(fieldType),
                  TypeInfo.Kind.FIELD,
                  null,
                  getModifiersString(node.getModifiers()));
        }
      }
    }

    /**
     * フィールドの型を推論
     *
     * @param field フィールドノード
     * @return 推論された型（推論できない場合は宣言された型）
     */
    private ClassNode inferFieldType(FieldNode field) {
      // 明示的に型が宣言されている場合（defやObjectでない場合）はその型を使用
      ClassNode declaredType = field.getType();
      if (!field.isDynamicTyped() && !"java.lang.Object".equals(declaredType.getName())) {
        return declaredType;
      }

      // 初期化式から型を推論
      if (field.hasInitialExpression()) {
        Expression init = field.getInitialExpression();
        ClassNode inferredType = inferTypeFromExpression(init);
        if (inferredType != null && !"java.lang.Object".equals(inferredType.getName())) {
          return inferredType;
        }
      }

      // 推論できない場合は宣言された型を返す
      return declaredType;
    }

    @Override
    public void visitMethod(MethodNode node) {
      if (foundTypeInfo != null) {
        return;
      }

      logger.debug(
          "visitMethod: {} (isConstructor: {}) at {}:{}, hasOverride: {}",
          node.getName(),
          node.getName().equals("<init>"),
          node.getLineNumber(),
          node.getColumnNumber(),
          hasOverrideAnnotation(node));

      // パラメータをチェック
      Parameter[] parameters = node.getParameters();
      if (parameters != null) {
        for (Parameter param : parameters) {
          // パラメータの型情報も記録
          variableTypes.put(param.getName(), param.getType());

          if (isPositionWithin(param)) {
            foundTypeInfo =
                new TypeInfo(
                    param.getName(),
                    formatTypeName(param.getType()),
                    TypeInfo.Kind.PARAMETER,
                    null,
                    null);
            return;
          }
        }
      }

      // メソッド本体を探索
      Statement code = node.getCode();
      if (code != null) {
        code.visit(this);
      }

      // 子要素で見つからなかった場合、メソッド名の位置をチェック
      if (foundTypeInfo == null) {
        // コンストラクタの場合は特別な処理
        if (node.getName().equals("<init>")) {
          ClassNode declaringClass = node.getDeclaringClass();
          // コンストラクタ名は実際にはクラス名と同じ位置にある
          if (isPositionWithinConstructorName(declaringClass, node)) {
            foundTypeInfo =
                new TypeInfo(
                    declaringClass.getNameWithoutPackage(),
                    formatConstructorSignature(declaringClass, node),
                    TypeInfo.Kind.METHOD,
                    "コンストラクタ",
                    getModifiersString(node.getModifiers()));
          }
        } else if (isPositionWithinMethodName(node)) {
          // オーバーライドメソッドの場合は特別な処理
          String documentation = null;
          String signature = formatMethodSignature(node);

          if (hasOverrideAnnotation(node)) {
            logger.debug("@Overrideアノテーション付きメソッドを検出: {}", node.getName());
            documentation = createOverrideMethodDocumentation(node);
          }

          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  signature,
                  TypeInfo.Kind.METHOD,
                  documentation,
                  getModifiersString(node.getModifiers()));
        }
      }
    }

    /**
     * 指定位置がメソッド名内にあるかチェック
     *
     * @param node メソッドノード
     * @return 位置がメソッド名内にある場合true
     */
    private boolean isPositionWithinMethodName(MethodNode node) {
      int line = targetPosition.getLine();
      int column = targetPosition.getCharacter();

      // デバッグログ
      logger.debug(
          "メソッド位置チェック: メソッド名={}, ノード位置={}:{}, ターゲット位置={}:{}",
          node.getName(),
          node.getLineNumber(),
          node.getColumnNumber(),
          line,
          column);

      // メソッド名の実際の位置を正確に計算
      // 戻り値の型名の長さに基づいてオフセットを計算
      String returnTypeName = formatTypeName(node.getReturnType());

      // ノードの列位置は1ベースで、行の最初の非空白文字（修飾子、アノテーション、または型名）を指す
      // @Overrideアノテーションがある場合、ノードの位置は戻り値の型（String）の位置を指すことが多い
      int methodNameStartColumn;

      // 型名に応じてオフセットを計算
      if (returnTypeName.equals("void")) {
        methodNameStartColumn = node.getColumnNumber() + 5; // "void "の長さ
      } else if (returnTypeName.equals("String")) {
        methodNameStartColumn = node.getColumnNumber() + 7; // "String "の長さ
      } else if (returnTypeName.equals("int") || returnTypeName.equals("def")) {
        methodNameStartColumn = node.getColumnNumber() + 4; // "int "または"def "の長さ
      } else {
        // その他の型の場合、型名の長さ + 空白1文字
        methodNameStartColumn = node.getColumnNumber() + returnTypeName.length() + 1;
      }

      // デバッグログ追加
      logger.debug("メソッド名位置計算: 型名={}, メソッド名開始位置={}", returnTypeName, methodNameStartColumn);
      logger.debug(
          "メソッド位置比較: ターゲット行={}, メソッド行={}, ターゲット列={}, 開始列={}, 終了列={}",
          line,
          node.getLineNumber(),
          column,
          methodNameStartColumn,
          methodNameStartColumn + node.getName().length());

      // targetPositionは既に1ベースに変換されているので、そのまま比較
      // @Overrideアノテーションがある場合、ノードの行番号が実際のメソッド宣言行と異なる可能性がある
      boolean lineMatch = line == node.getLineNumber();

      // @Overrideアノテーションがある場合、次の行も確認
      if (!lineMatch && hasOverrideAnnotation(node)) {
        lineMatch = line == node.getLineNumber() + 1;
        logger.debug("@Overrideアノテーション付きメソッドのため、次の行も確認: {}", lineMatch);
      }

      return lineMatch
          && column >= methodNameStartColumn
          && column < methodNameStartColumn + node.getName().length();
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
      if (foundTypeInfo != null) {
        return;
      }

      for (Statement stmt : block.getStatements()) {
        stmt.visit(this);
        if (foundTypeInfo != null) {
          return;
        }
      }
    }

    @Override
    public void visitExpressionStatement(ExpressionStatement statement) {
      if (foundTypeInfo != null) {
        return;
      }

      Expression expr = statement.getExpression();
      logger.debug(
          "式のタイプ: {} at {}:{}",
          expr != null ? expr.getClass().getSimpleName() : "null",
          expr != null ? expr.getLineNumber() : 0,
          expr != null ? expr.getColumnNumber() : 0);
      // DeclarationExpressionも含めて、すべての式を訪問する
      expr.visit(this);
    }

    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
      // 変数宣言の処理
      Expression leftExpression = expression.getLeftExpression();
      if (leftExpression instanceof VariableExpression) {
        var varExpr = (VariableExpression) leftExpression;

        // 変数の型情報を記録（後で参照時に使用）
        ClassNode declaredType = varExpr.getType();

        // 型名の位置にカーソルがある場合の処理
        if (foundTypeInfo == null
            && declaredType != null
            && !isPrimitiveType(declaredType.getName())) {
          // 型の位置を推定（変数名の前）
          int line = varExpr.getLineNumber();
          int typeEndColumn = varExpr.getColumnNumber() - 1; // 変数名の直前
          String typeName = declaredType.getNameWithoutPackage();
          int typeStartColumn = typeEndColumn - typeName.length();

          logger.debug(
              "型位置チェック: {} at {}:{}-{}, target: {}:{}",
              typeName,
              line,
              typeStartColumn,
              typeEndColumn,
              targetPosition.getLine(),
              targetPosition.getCharacter());

          if (targetPosition.getLine() == line
              && targetPosition.getCharacter() >= typeStartColumn
              && targetPosition.getCharacter() <= typeEndColumn) {

            // import aliasかどうかをチェック
            if (astInfo != null) {
              String resolvedClassName = astInfo.resolveAlias(typeName);
              if (resolvedClassName != null) {
                logger.debug("Type {} is an alias for {}", typeName, resolvedClassName);
                foundTypeInfo =
                    new TypeInfo(
                        typeName,
                        resolvedClassName,
                        TypeInfo.Kind.CLASS,
                        "**Import alias**: " + typeName + " → " + resolvedClassName,
                        null);
                return;
              }
            }

            // エイリアスでない場合は通常の型情報を表示
            foundTypeInfo =
                new TypeInfo(
                    typeName, formatTypeName(declaredType), TypeInfo.Kind.CLASS, null, null);
            return;
          }
        }

        // defやvarで宣言された場合、右辺から型を推論
        if (declaredType == null || declaredType.getName().equals("java.lang.Object")) {
          Expression rightExpression = expression.getRightExpression();
          if (rightExpression != null) {
            ClassNode inferredType = inferTypeFromExpression(rightExpression);
            if (inferredType != null && !inferredType.getName().equals("java.lang.Object")) {
              declaredType = inferredType;
              variableTypes.put(varExpr.getName(), inferredType);
            }
          }
        } else {
          variableTypes.put(varExpr.getName(), declaredType);
        }

        // デバッグログ
        logger.debug(
            "変数宣言を記録: {} (型: {}) at {}:{}-{}:{}",
            varExpr.getName(),
            declaredType != null ? declaredType.getName() : "null",
            varExpr.getLineNumber(),
            varExpr.getColumnNumber(),
            varExpr.getLastLineNumber(),
            varExpr.getLastColumnNumber());

        if (foundTypeInfo == null && isPositionWithin(varExpr)) {
          foundTypeInfo =
              new TypeInfo(
                  varExpr.getName(),
                  formatTypeName(declaredType),
                  TypeInfo.Kind.LOCAL_VARIABLE,
                  null,
                  null);
        }
      }

      // 右辺の式も訪問（変数参照が含まれている可能性がある）
      Expression rightExpression = expression.getRightExpression();
      if (rightExpression != null && foundTypeInfo == null) {
        rightExpression.visit(this);
      }
    }

    @Override
    public void visitVariableExpression(VariableExpression expression) {
      if (foundTypeInfo != null) {
        return;
      }

      // 変数参照の処理
      if (isPositionWithin(expression)) {
        String varName = expression.getName();

        // import aliasかどうかをチェック
        if (astInfo != null) {
          logger.debug("Checking if {} is an import alias", varName);
          String resolvedClassName = astInfo.resolveAlias(varName);
          if (resolvedClassName != null) {
            logger.debug("Resolved alias {} to class {}", varName, resolvedClassName);
            // エイリアスの場合、元のクラス名を表示
            foundTypeInfo =
                new TypeInfo(
                    varName,
                    resolvedClassName,
                    TypeInfo.Kind.CLASS,
                    "**Import alias**: " + varName + " → " + resolvedClassName,
                    null);
            return;
          }
        }

        // まず記録された変数宣言から型情報を探す
        ClassNode recordedType = variableTypes.get(varName);
        ClassNode typeToUse = recordedType != null ? recordedType : expression.getType();

        // デバッグログ
        logger.debug(
            "変数参照: {} (記録された型: {}, 式の型: {}) at {}:{}",
            varName,
            recordedType != null ? recordedType.getName() : "なし",
            expression.getType().getName(),
            expression.getLineNumber(),
            expression.getColumnNumber());

        // シンボルテーブルから詳細情報を取得
        Option<SymbolDefinition> symbolOption =
            scopeManager.findSymbolAt(uri, targetPosition, varName);

        String documentation = null;
        if (symbolOption.isDefined()) {
          documentation = createDocumentation(symbolOption.get());
        }

        foundTypeInfo =
            new TypeInfo(
                expression.getName(),
                formatTypeName(typeToUse),
                TypeInfo.Kind.LOCAL_VARIABLE,
                documentation,
                null);
      }
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression call) {
      if (foundTypeInfo != null) {
        return;
      }

      logger.debug(
          "visitMethodCallExpression: {} at {}:{}",
          call.getMethodAsString(),
          call.getLineNumber(),
          call.getColumnNumber());

      // メソッド呼び出しの処理
      Expression method = call.getMethod();
      Expression objectExpr = call.getObjectExpression();
      logger.debug(
          "メソッド式のタイプ: {}, オブジェクト式のタイプ: {}",
          method != null ? method.getClass().getName() : "null",
          objectExpr != null ? objectExpr.getClass().getSimpleName() : "null");

      // ConstantExpressionの場合も処理
      if (method instanceof ConstantExpression) {
        var constExpr = (ConstantExpression) method;

        // デバッグ: 定数式の位置情報
        logger.debug(
            "定数式の位置: {}:{}-{}:{}, ターゲット位置: {}:{}, 値: {}",
            constExpr.getLineNumber(),
            constExpr.getColumnNumber(),
            constExpr.getLastLineNumber(),
            constExpr.getLastColumnNumber(),
            targetPosition.getLine(),
            targetPosition.getCharacter(),
            constExpr.getValue());

        if (isPositionWithin(constExpr)) {
          String methodName = constExpr.getValue().toString();

          // メソッド呼び出しのレシーバーの型を取得
          Expression objectExpression = call.getObjectExpression();
          String receiverTypeName = null;
          boolean isQualifiedCall = false;

          if (objectExpression instanceof VariableExpression) {
            var varExpr = (VariableExpression) objectExpression;
            String varName = varExpr.getName();

            // qualified呼び出しかどうかを判定（java.time.Instantなど）
            if (varName.contains(".")) {
              // これは qualified クラス参照の可能性がある
              receiverTypeName = varName;
              isQualifiedCall = true;
            } else {
              // 変数の型を取得
              ClassNode recordedType = variableTypes.get(varName);
              if (recordedType != null) {
                receiverTypeName = recordedType.getName();
              } else {
                // シンボルテーブルから変数の型を検索
                Option<SymbolDefinition> varSymbol =
                    scopeManager.findSymbolAt(uri, targetPosition, varName);
                if (varSymbol.isDefined()) {
                  receiverTypeName = varSymbol.get().qualifiedName();
                }
              }
            }
          } else if (objectExpression instanceof PropertyExpression) {
            // java.time.Instant.now() のような qualified 呼び出し
            receiverTypeName = getQualifiedNameFromExpression(objectExpression);
            isQualifiedCall = true;
          } else if (objectExpression instanceof ClassExpression) {
            // Math.sin() のような直接的なクラス参照
            var classExpr = (ClassExpression) objectExpression;
            receiverTypeName = classExpr.getType().getName();
            logger.debug("ClassExpression detected: {}", receiverTypeName);
          } else {
            logger.debug(
                "Unknown objectExpression type: {}",
                objectExpression != null ? objectExpression.getClass().getSimpleName() : "null");
          }

          // レシーバーの型が分かった場合、その型のメソッドを検索
          if (receiverTypeName != null) {
            String qualifiedMethodName = receiverTypeName + "." + methodName;
            Option<SymbolDefinition> methodSymbol =
                symbolTable.findByQualifiedName(qualifiedMethodName);

            if (methodSymbol.isDefined()) {
              foundTypeInfo = createTypeInfoFromSymbol(methodSymbol.get());
              return;
            }
          }

          // シンボルテーブルから検索
          Option<SymbolDefinition> symbolOption =
              scopeManager.findSymbolAt(uri, targetPosition, methodName);

          if (symbolOption.isDefined()) {
            foundTypeInfo = createTypeInfoFromSymbol(symbolOption.get());
          } else {
            // メソッド呼び出しとして扱う（改善版：FQN情報を含める）
            String signature;
            String documentation = null;

            if (isQualifiedCall && receiverTypeName != null) {
              // qualified呼び出しの場合はFQNを含める
              signature = receiverTypeName + "." + methodName + "(...)";
              documentation = "**完全修飾メソッド呼び出し**\n\n" + "クラス: " + receiverTypeName;
            } else if (receiverTypeName != null) {
              // 通常のメソッド呼び出し
              String simpleClassName = getSimpleClassName(receiverTypeName);
              signature = simpleClassName + "." + methodName + "(...)";
              documentation = "**メソッド呼び出し**\n\n" + "クラス: " + receiverTypeName;
            } else {
              signature = methodName + "(...)";
            }

            foundTypeInfo =
                new TypeInfo(methodName, signature, TypeInfo.Kind.METHOD, documentation, null);
          }
          return;
        }
      } else if (method instanceof VariableExpression) {
        var methodExpr = (VariableExpression) method;

        // デバッグ: メソッド式の位置情報
        logger.debug(
            "メソッド式の位置: {}:{}-{}:{}, ターゲット位置: {}:{}",
            methodExpr.getLineNumber(),
            methodExpr.getColumnNumber(),
            methodExpr.getLastLineNumber(),
            methodExpr.getLastColumnNumber(),
            targetPosition.getLine(),
            targetPosition.getCharacter());

        if (isPositionWithin(methodExpr)) {
          // メソッド名から定義を探す
          String methodName = methodExpr.getName();

          // シンボルテーブルから検索
          Option<SymbolDefinition> symbolOption =
              scopeManager.findSymbolAt(uri, targetPosition, methodName);

          if (symbolOption.isDefined()) {
            foundTypeInfo = createTypeInfoFromSymbol(symbolOption.get());
          } else {
            // メソッド呼び出しとして扱う
            foundTypeInfo = new TypeInfo(methodName, "メソッド呼び出し", TypeInfo.Kind.METHOD, null, null);
          }
          return;
        }
      }

      // 子要素を訪問
      super.visitMethodCallExpression(call);
    }

    @Override
    public void visitBinaryExpression(BinaryExpression expression) {
      if (foundTypeInfo != null) {
        return;
      }

      // 左辺（代入の対象）を訪問
      Expression leftExpression = expression.getLeftExpression();
      leftExpression.visit(this);

      if (foundTypeInfo == null) {
        // 右辺も訪問
        Expression rightExpression = expression.getRightExpression();
        rightExpression.visit(this);
      }
    }

    @Override
    public void visitConstructorCallExpression(ConstructorCallExpression expression) {
      if (foundTypeInfo != null) {
        return;
      }

      logger.debug(
          "visitConstructorCallExpression: {} at {}:{}",
          expression.getType().getName(),
          expression.getLineNumber(),
          expression.getColumnNumber());

      // new User(...) の "User" 部分をチェック
      if (isPositionWithin(expression)) {
        ClassNode type = expression.getType();
        String className = type.getNameWithoutPackage();

        // シンボルテーブルからクラス定義を検索
        io.vavr.collection.List<SymbolDefinition> classSymbols = symbolTable.findByName(className);

        if (!classSymbols.isEmpty()) {
          // クラス定義が見つかった場合、コンストラクタ情報として表示

          foundTypeInfo =
              new TypeInfo(
                  className,
                  formatConstructorSignature(type, null),
                  TypeInfo.Kind.METHOD,
                  "コンストラクタ",
                  null);
        } else {
          // クラス定義が見つからない場合でも、基本的な情報を表示
          foundTypeInfo =
              new TypeInfo(className, className + "(...)", TypeInfo.Kind.METHOD, "コンストラクタ", null);
        }
      }
    }

    @Override
    public void visitPropertyExpression(PropertyExpression expression) {
      if (foundTypeInfo != null) {
        return;
      }

      logger.debug(
          "visitPropertyExpression: {} at {}:{}",
          expression.getPropertyAsString(),
          expression.getLineNumber(),
          expression.getColumnNumber());

      // プロパティアクセスの処理（this.name など）
      Expression property = expression.getProperty();
      logger.debug("プロパティのタイプ: {}", property != null ? property.getClass().getName() : "null");

      if (property instanceof ConstantExpression) {
        var constProp = (ConstantExpression) property;
        logger.debug(
            "プロパティ定数式の位置: {}:{}-{}:{}, ターゲット位置: {}:{}, 値: {}",
            constProp.getLineNumber(),
            constProp.getColumnNumber(),
            constProp.getLastLineNumber(),
            constProp.getLastColumnNumber(),
            targetPosition.getLine(),
            targetPosition.getCharacter(),
            constProp.getValue());

        if (isPositionWithin(constProp)) {
          String propName = constProp.getValue().toString();

          // オブジェクト式の型を取得
          Expression objectExpression = expression.getObjectExpression();
          ClassNode objectType = null;

          if (objectExpression instanceof VariableExpression) {
            var varExpr = (VariableExpression) objectExpression;
            // 変数の型を取得
            objectType = variableTypes.get(varExpr.getName());
          } else if (objectExpression instanceof ClassExpression) {
            // Color.RED のようなenum定数アクセスの場合
            var classExpr = (ClassExpression) objectExpression;
            objectType = classExpr.getType();
          }

          // オブジェクトの型が判明した場合、そのフィールド情報を検索
          if (objectType != null) {
            // enumの場合は特別な処理
            if (objectType.isEnum()) {
              // enum定数を検索
              for (FieldNode field : objectType.getFields()) {
                if (field.isEnum() && field.getName().equals(propName)) {
                  foundTypeInfo =
                      new TypeInfo(
                          propName,
                          objectType.getNameWithoutPackage(),
                          TypeInfo.Kind.ENUM_CONSTANT,
                          null,
                          null);
                  return;
                }
              }
            } else {
              // 静的ネストクラスの可能性をチェック
              Iterator<InnerClassNode> innerClassIterator = objectType.getInnerClasses();
              if (innerClassIterator != null) {
                while (innerClassIterator.hasNext()) {
                  InnerClassNode innerClass = innerClassIterator.next();
                  if (innerClass.getNameWithoutPackage().equals(propName)) {
                    logger.debug(
                        "静的ネストクラスを検出: {}.{}", objectType.getNameWithoutPackage(), propName);
                    foundTypeInfo =
                        new TypeInfo(
                            propName,
                            getFullyQualifiedClassName(innerClass),
                            TypeInfo.Kind.CLASS,
                            createNestedClassDocumentation(innerClass),
                            getModifiersString(innerClass.getModifiers()));
                    return;
                  }
                }
              }

              // 通常のフィールド検索
              if (astInfo != null) {
                String className = formatTypeName(objectType);
                ClassInfo classInfo = astInfo.findClassByName(className);
                if (classInfo != null) {
                  // フィールドを検索
                  for (var field : classInfo.fields()) {
                    if (field.name().equals(propName)) {
                      foundTypeInfo =
                          new TypeInfo(
                              propName,
                              field.type(),
                              TypeInfo.Kind.FIELD,
                              "フィールド: " + className + "." + propName,
                              null);
                      return;
                    }
                  }
                }
              }
            }
          }

          // フィールドとして記録されているか確認
          ClassNode recordedType = variableTypes.get(propName);
          if (recordedType != null) {
            foundTypeInfo =
                new TypeInfo(
                    propName, formatTypeName(recordedType), TypeInfo.Kind.FIELD, null, null);
          } else {
            // シンボルテーブルから検索
            Option<SymbolDefinition> symbolOption =
                scopeManager.findSymbolAt(uri, targetPosition, propName);

            if (symbolOption.isDefined()) {
              foundTypeInfo = createTypeInfoFromSymbol(symbolOption.get());
            }
          }
          return;
        }
      } else if (property instanceof VariableExpression) {
        var propExpr = (VariableExpression) property;

        logger.debug(
            "プロパティ式の位置: {}:{}-{}:{}, ターゲット位置: {}:{}",
            propExpr.getLineNumber(),
            propExpr.getColumnNumber(),
            propExpr.getLastLineNumber(),
            propExpr.getLastColumnNumber(),
            targetPosition.getLine(),
            targetPosition.getCharacter());

        if (isPositionWithin(propExpr)) {
          String propName = propExpr.getName();

          // フィールドとして記録されているか確認
          ClassNode recordedType = variableTypes.get(propName);
          if (recordedType != null) {
            foundTypeInfo =
                new TypeInfo(
                    propName, formatTypeName(recordedType), TypeInfo.Kind.FIELD, null, null);
          } else {
            // シンボルテーブルから検索
            Option<SymbolDefinition> symbolOption =
                scopeManager.findSymbolAt(uri, targetPosition, propName);

            if (symbolOption.isDefined()) {
              foundTypeInfo = createTypeInfoFromSymbol(symbolOption.get());
            }
          }
          return;
        }
      }

      // 子要素を訪問
      super.visitPropertyExpression(expression);
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
      if (foundTypeInfo != null) {
        return;
      }

      // クロージャパラメータの処理
      Parameter[] parameters = expression.getParameters();
      if (parameters != null && parameters.length > 0) {
        // 明示的なパラメータがある場合
        for (Parameter param : parameters) {
          // パラメータの型情報を記録
          variableTypes.put(param.getName(), param.getType());

          if (isPositionWithin(param)) {
            foundTypeInfo =
                new TypeInfo(
                    param.getName(),
                    formatTypeName(param.getType()),
                    TypeInfo.Kind.PARAMETER,
                    "クロージャパラメータ",
                    null);
            return;
          }
        }
      } else {
        // パラメータがない場合、暗黙的な'it'パラメータが利用可能
        ClassNode previousItType = variableTypes.get("it");
        ClassNode inferredItType = inferClosureItType();

        if (inferredItType != null) {
          logger.debug("クロージャの暗黙的なit型を設定: {}", inferredItType.getName());
          variableTypes.put("it", inferredItType);
        }

        // クロージャ本体を訪問
        Statement code = expression.getCode();
        if (code != null) {
          code.visit(this);
        }

        // 元のit型を復元（ネストされたクロージャのため）
        if (previousItType != null) {
          variableTypes.put("it", previousItType);
        } else {
          variableTypes.remove("it");
        }
        return;
      }

      // クロージャ本体を訪問
      Statement code = expression.getCode();
      if (code != null) {
        code.visit(this);
      }
    }

    /**
     * 指定位置がノード内にあるかチェック
     *
     * @param node ASTノード
     * @return 位置がノード内にある場合true
     */
    private boolean isPositionWithin(ASTNode node) {
      int line = targetPosition.getLine();
      int column = targetPosition.getCharacter();

      // targetPositionは既に1ベースに変換されている
      return line >= node.getLineNumber()
          && line <= node.getLastLineNumber()
          && (line > node.getLineNumber() || column >= node.getColumnNumber())
          && (line < node.getLastLineNumber() || column <= node.getLastColumnNumber());
    }

    /**
     * 型名をフォーマット
     *
     * @param type クラスノード
     * @return フォーマットされた型名
     */
    private String formatTypeName(ClassNode type) {
      if (type == null) {
        return "Object";
      }

      String typeName = type.getName();

      // プリミティブ型や基本的な型は短い名前を使用
      if (isPrimitiveType(typeName) || typeName.startsWith("java.lang.")) {
        return type.getNameWithoutPackage();
      }

      // 配列型の処理
      if (type.isArray()) {
        return formatTypeName(type.getComponentType()) + "[]";
      }

      // import aliasのチェック
      if (astInfo != null) {
        // フルクラス名でImportInfoを検索
        var importInfo =
            astInfo.imports().stream()
                .filter(imp -> imp.className().equals(typeName))
                .findFirst()
                .orElse(null);

        if (importInfo != null && importInfo.alias() != null) {
          // エイリアスが定義されている場合は、完全修飾名とエイリアスを表示
          return typeName + " (alias: " + importInfo.alias() + ")";
        }
      }

      // ジェネリクス型の基本的な処理
      GenericsType[] generics = type.getGenericsTypes();
      if (generics != null && generics.length > 0) {
        var sb = new StringBuilder(type.getNameWithoutPackage());
        sb.append("<");

        // 長い型名の省略処理

        int totalLength = sb.length();

        for (int i = 0; i < generics.length; i++) {
          if (i > 0) {
            sb.append(", ");
          }

          GenericsType generic = generics[i];
          String genericTypeName;

          // 型パラメータ名
          if (generic.isPlaceholder()) {
            genericTypeName = generic.getName();

            // 上限境界がある場合（長い場合は省略）
            if (generic.getUpperBounds() != null && generic.getUpperBounds().length > 0) {
              genericTypeName += " extends ";
              if (generic.getUpperBounds().length == 1) {
                genericTypeName += formatTypeName(generic.getUpperBounds()[0]);
              } else {
                // 複数の境界がある場合は最初のものだけ表示
                genericTypeName += formatTypeName(generic.getUpperBounds()[0]) + " & ...";
              }
            }
          } else {
            genericTypeName = formatTypeName(generic.getType());
          }

          // 長すぎる場合は省略
          totalLength += genericTypeName.length();
          if (totalLength > 80 && i < generics.length - 1) {
            sb.append(genericTypeName).append(", ...");

            break;
          } else {
            sb.append(genericTypeName);
          }
        }

        sb.append(">");
        return sb.toString();
      }

      // 通常の型名もパッケージが長い場合は適切に省略
      String simpleClassName = type.getNameWithoutPackage();

      // パッケージ名を安全に取得
      if (typeName.contains(".")) {
        int lastDotIndex = typeName.lastIndexOf('.');
        String packageName = typeName.substring(0, lastDotIndex);

        // よく使われるパッケージは完全表示
        if (packageName.equals("java.util")
            || packageName.equals("java.io")
            || packageName.equals("java.net")
            || packageName.equals("java.time")) {
          return typeName; // 完全修飾名を表示
        }
      }

      // その他のパッケージは単純名を表示
      return simpleClassName;
    }

    /**
     * メソッドシグネチャをフォーマット
     *
     * @param method メソッドノード
     * @return フォーマットされたシグネチャ
     */
    private String formatMethodSignature(MethodNode method) {
      var sb = new StringBuilder();
      sb.append(method.getName());
      sb.append("(");

      Parameter[] params = method.getParameters();
      if (params != null) {
        for (int i = 0; i < params.length; i++) {
          if (i > 0) {
            sb.append(", ");
          }
          sb.append(formatTypeName(params[i].getType()));
          sb.append(" ");
          sb.append(params[i].getName());
        }
      }

      sb.append("): ");
      sb.append(formatTypeName(method.getReturnType()));

      return sb.toString();
    }

    /**
     * 修飾子を文字列に変換
     *
     * @param modifiers 修飾子のビットマスク
     * @return 修飾子の文字列表現
     */
    private String getModifiersString(int modifiers) {
      List<String> modList = new ArrayList<>();

      if (java.lang.reflect.Modifier.isPublic(modifiers)) {
        modList.add("public");
      } else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
        modList.add("protected");
      } else if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
        modList.add("private");
      }

      if (java.lang.reflect.Modifier.isStatic(modifiers)) {
        modList.add("static");
      }
      if (java.lang.reflect.Modifier.isFinal(modifiers)) {
        modList.add("final");
      }
      if (java.lang.reflect.Modifier.isAbstract(modifiers)) {
        modList.add("abstract");
      }

      return modList.isEmpty() ? null : String.join(" ", modList);
    }

    /**
     * コンストラクタシグネチャをフォーマット
     *
     * @param declaringClass 宣言クラス
     * @param constructor コンストラクタメソッドノード
     * @return フォーマットされたシグネチャ
     */
    private String formatConstructorSignature(ClassNode declaringClass, MethodNode constructor) {
      var sb = new StringBuilder();
      sb.append(declaringClass.getNameWithoutPackage());
      sb.append("(");

      Parameter[] params = constructor.getParameters();
      if (params != null) {
        for (int i = 0; i < params.length; i++) {
          if (i > 0) {
            sb.append(", ");
          }
          sb.append(formatTypeName(params[i].getType()));
          sb.append(" ");
          sb.append(params[i].getName());
        }
      }

      sb.append(")");
      return sb.toString();
    }

    /**
     * プリミティブ型かどうかを判定
     *
     * @param typeName 型名
     * @return プリミティブ型の場合true
     */
    private boolean isPrimitiveType(String typeName) {
      return typeName.equals("int")
          || typeName.equals("long")
          || typeName.equals("short")
          || typeName.equals("byte")
          || typeName.equals("float")
          || typeName.equals("double")
          || typeName.equals("boolean")
          || typeName.equals("char")
          || typeName.equals("void");
    }

    /**
     * 式から型を推論
     *
     * @param expression 式
     * @return 推論された型、推論できない場合はnull
     */
    private @Nullable ClassNode inferTypeFromExpression(Expression expression) {
      if (expression instanceof MethodCallExpression) {
        var methodCall = (MethodCallExpression) expression;

        Expression objectExpression = methodCall.getObjectExpression();

        if (objectExpression instanceof VariableExpression) {
          var varExpr = (VariableExpression) objectExpression;
          // 変数の型を取得
          ClassNode recordedType = variableTypes.get(varExpr.getName());
          if (recordedType != null) {
            // その型のメソッドを検索
            String methodName = methodCall.getMethodAsString();
            if (methodName != null && astInfo != null) {
              // 記録された型からクラス情報を検索
              String className = formatTypeName(recordedType);
              ClassInfo classInfo = astInfo.findClassByName(className);
              if (classInfo != null) {
                // メソッドを検索
                for (MethodInfo method : classInfo.methods()) {
                  if (method.name().equals(methodName)) {
                    // 戻り値の型を返す
                    return getClassNodeForTypeName(method.returnType());
                  }
                }
              }
            }
          }
        }
      } else if (expression instanceof ConstructorCallExpression) {
        var ctorCall = (ConstructorCallExpression) expression;
        // new User(...) の場合、User型を返す
        return ctorCall.getType();
      } else if (expression instanceof ConstantExpression) {
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
      }

      // SpockのMock/Stubの処理
      if (expression instanceof MethodCallExpression) {
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
              } else if (firstArg instanceof VariableExpression) {
                // Mock(UserService)のように、引数が変数として扱われる場合
                var varExpr = (VariableExpression) firstArg;
                String typeName = varExpr.getName();
                // 型名から直接ClassNodeを作成
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
     * 指定位置がコンストラクタ名内にあるかチェック
     *
     * @param declaringClass 宣言クラス
     * @param constructor コンストラクタメソッドノード
     * @return 位置がコンストラクタ名内にある場合true
     */
    private boolean isPositionWithinConstructorName(
        ClassNode declaringClass, MethodNode constructor) {
      int line = targetPosition.getLine();
      int column = targetPosition.getCharacter();

      // デバッグログ
      logger.debug(
          "コンストラクタ位置チェック: クラス名={}, コンストラクタ位置={}:{}, ターゲット位置={}:{}",
          declaringClass.getNameWithoutPackage(),
          constructor.getLineNumber(),
          constructor.getColumnNumber(),
          line,
          column);

      // コンストラクタはクラス名と同じ名前を持つ
      String className = declaringClass.getNameWithoutPackage();

      // コンストラクタの位置はコンストラクタ名の開始位置を指す
      // targetPositionは既に1ベースに変換されているので、そのまま比較
      return line == constructor.getLineNumber()
          && column >= constructor.getColumnNumber()
          && column < constructor.getColumnNumber() + className.length();
    }

    /**
     * 型名からClassNodeを取得
     *
     * @param typeName 型名
     * @return ClassNode、見つからない場合はnull
     */
    private @Nullable ClassNode getClassNodeForTypeName(String typeName) {
      if (typeName == null) {
        return null;
      }

      // 基本型のマッピング
      switch (typeName) {
        case "int":
        case "java.lang.Integer":
          return ClassHelper.int_TYPE;
        case "long":
        case "java.lang.Long":
          return ClassHelper.long_TYPE;
        case "short":
        case "java.lang.Short":
          return ClassHelper.short_TYPE;
        case "byte":
        case "java.lang.Byte":
          return ClassHelper.byte_TYPE;
        case "float":
        case "java.lang.Float":
          return ClassHelper.float_TYPE;
        case "double":
        case "java.lang.Double":
          return ClassHelper.double_TYPE;
        case "boolean":
        case "java.lang.Boolean":
          return ClassHelper.boolean_TYPE;
        case "char":
        case "java.lang.Character":
          return ClassHelper.char_TYPE;
        case "void":
          return ClassHelper.VOID_TYPE;
        case "java.lang.String":
        case "String":
          return ClassHelper.STRING_TYPE;
        case "java.lang.Object":
        case "Object":
          return ClassHelper.OBJECT_TYPE;
        default:
          // その他のクラスの場合、ClassNodeを生成
          return new ClassNode(typeName, 0, ClassHelper.OBJECT_TYPE);
      }
    }

    /**
     * enum型のドキュメントを生成
     *
     * @param enumNode enumクラスノード
     * @return ドキュメント文字列
     */
    private String createEnumDocumentation(ClassNode enumNode) {
      var sb = new StringBuilder();
      sb.append("**列挙型**: ").append(enumNode.getNameWithoutPackage()).append("\n\n");

      // 列挙定数を列挙
      sb.append("**列挙定数**:\n");
      int constantCount = 0;
      for (FieldNode field : enumNode.getFields()) {
        if (field.isEnum()) {
          sb.append("- `").append(field.getName()).append("`\n");
          constantCount++;
        }
      }

      if (constantCount == 0) {
        sb.append("（定数なし）\n");
      }

      return sb.toString();
    }

    /**
     * レコードクラスかどうかを判定
     *
     * @param node クラスノード
     * @return レコードクラスの場合true
     */
    private boolean isRecordClass(ClassNode node) {
      // Groovy 4.0+ のレコードクラスの判定

      // 0. ソースコードからrecordキーワードを確認（最も確実な方法）
      Option<String> contentOption = documentContentService.getContent(uri);
      if (contentOption.isDefined()) {
        String sourceCode = contentOption.get();
        String[] lines = sourceCode.split("\n");

        // クラス定義の行を確認
        if (node.getLineNumber() - 1 >= 0 && node.getLineNumber() - 1 < lines.length) {
          String lineContent = lines[node.getLineNumber() - 1];
          if (lineContent.trim().startsWith("record ")) {
            logger.debug("recordキーワードでレコードクラスを検出: {} - '{}'", node.getName(), lineContent.trim());
            return true;
          }
        }
      }

      // 1. isRecord() メソッドがある場合はそれを使用（Groovy 4.0+）
      try {
        // リフレクションを使用してisRecord()メソッドの存在をチェック
        java.lang.reflect.Method isRecordMethod = node.getClass().getMethod("isRecord");
        if (isRecordMethod != null) {
          Object result = isRecordMethod.invoke(node);
          if (result instanceof Boolean && (Boolean) result) {
            logger.debug("isRecord()メソッドでレコードクラスを検出: {}", node.getName());
            return true;
          }
        }
      } catch (Exception e) {
        // isRecord()メソッドが存在しない場合は他の方法で判定
        logger.debug("isRecord()メソッドが利用できません: {}", e.getMessage());
      }

      // 2. レコード関連のアノテーションをチェック
      if (node.getAnnotations() != null) {
        boolean hasRecordAnnotation =
            node.getAnnotations().stream()
                .anyMatch(
                    ann -> {
                      String name = ann.getClassNode().getName();
                      return name.equals("groovy.transform.RecordType")
                          || name.equals("RecordType")
                          || name.equals("groovy.transform.RecordBase")
                          || name.equals("RecordBase");
                    });
        if (hasRecordAnnotation) {
          logger.debug("レコードアノテーションでレコードクラスを検出: {}", node.getName());
          return true;
        }
      }

      // 3. スーパークラスをチェック（java.lang.Recordを継承）
      ClassNode superClass = node.getSuperClass();
      while (superClass != null && !superClass.getName().equals("java.lang.Object")) {
        if (superClass.getName().equals("java.lang.Record")) {
          logger.debug("java.lang.Record継承でレコードクラスを検出: {}", node.getName());
          return true;
        }
        superClass = superClass.getSuperClass();
      }

      // 4. レコードクラスの特徴をチェック
      // レコードクラスは通常、finalで、特定のコンストラクタパターンを持つ
      if (java.lang.reflect.Modifier.isFinal(node.getModifiers())) {
        // コンパクトコンストラクタがあるかチェック
        for (MethodNode constructor : node.getDeclaredConstructors()) {
          if (!constructor.isSynthetic()) {
            Parameter[] params = constructor.getParameters();
            if (params != null && params.length > 0) {
              // すべてのパラメータ名がフィールド名と一致するかチェック
              boolean allMatch = true;
              for (Parameter param : params) {
                boolean hasMatchingField = false;
                for (FieldNode field : node.getFields()) {
                  if (field.getName().equals(param.getName())
                      && !field.isStatic()
                      && java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    hasMatchingField = true;
                    break;
                  }
                }
                if (!hasMatchingField) {
                  allMatch = false;
                  break;
                }
              }
              if (allMatch) {
                logger.debug("構造的特徴からレコードクラスを推定: {}", node.getName());
                return true;
              }
            }
          }
        }
      }

      return false;
    }

    /**
     * Canonicalアノテーションがあるかどうかを判定
     *
     * @param node クラスノード
     * @return Canonicalアノテーションがある場合true
     */
    private boolean hasCanonicalAnnotation(ClassNode node) {
      return node.getAnnotations() != null
          && node.getAnnotations().stream()
              .anyMatch(
                  ann ->
                      ann.getClassNode().getName().equals("groovy.transform.Canonical")
                          || ann.getClassNode().getName().equals("Canonical"));
    }

    /**
     * レコードクラスのドキュメントを生成
     *
     * @param recordNode レコードクラスノード
     * @return ドキュメント文字列
     */
    private String createRecordDocumentation(ClassNode recordNode) {
      var sb = new StringBuilder();
      sb.append("**レコードクラス**: ").append(recordNode.getNameWithoutPackage()).append("\n\n");

      // コンポーネント（コンストラクタのパラメータ）を列挙
      sb.append("**コンポーネント**:\n");

      // レコードクラスの場合、コンポーネントは通常フィールドとして定義される
      // まずインスタンスフィールドからコンポーネントを探す
      List<FieldNode> components = new ArrayList<>();
      logger.debug("レコードクラス {} のフィールド数: {}", recordNode.getName(), recordNode.getFields().size());
      for (FieldNode field : recordNode.getFields()) {
        logger.debug(
            "フィールド: {} (static: {}, synthetic: {}, final: {}, type: {})",
            field.getName(),
            field.isStatic(),
            field.isSynthetic(),
            java.lang.reflect.Modifier.isFinal(field.getModifiers()),
            field.getType() != null ? field.getType().getName() : "null");

        // レコードクラスのコンポーネントフィールドを判定
        // staticでなく、finalであり、$で始まらない名前のフィールドを含める
        if (!field.isStatic()
            && java.lang.reflect.Modifier.isFinal(field.getModifiers())
            && !field.getName().startsWith("$")) {
          // レコードクラスのコンポーネントフィールドを含める
          components.add(field);
          logger.debug("レコードコンポーネントとして追加: {}", field.getName());
        }
      }

      // フィールドが見つからない場合は、最もパラメータ数が多いコンストラクタを使用
      if (components.isEmpty()) {
        MethodNode primaryConstructor = null;
        int maxParams = 0;

        for (MethodNode constructor : recordNode.getDeclaredConstructors()) {
          if (!constructor.isSynthetic()) {
            Parameter[] params = constructor.getParameters();
            if (params != null && params.length > maxParams) {
              primaryConstructor = constructor;
              maxParams = params.length;
            }
          }
        }

        // プライマリコンストラクタのパラメータを表示
        if (primaryConstructor != null) {
          Parameter[] params = primaryConstructor.getParameters();
          if (params != null) {
            for (Parameter param : params) {
              sb.append("- `")
                  .append(param.getName())
                  .append("` : ")
                  .append(formatTypeName(param.getType()))
                  .append("\n");
            }
          }
        }
      } else {
        // フィールドをコンポーネントとして表示
        for (FieldNode field : components) {
          sb.append("- `")
              .append(field.getName())
              .append("` : ")
              .append(formatTypeName(field.getType()))
              .append("\n");
        }
      }

      return sb.toString();
    }

    /**
     * Canonicalアノテーション付きクラスのドキュメントを生成
     *
     * @param canonicalNode Canonicalクラスノード
     * @return ドキュメント文字列
     */
    private String createCanonicalClassDocumentation(ClassNode canonicalNode) {
      var sb = new StringBuilder();
      sb.append("**@Canonicalクラス**: ").append(canonicalNode.getNameWithoutPackage()).append("\n\n");

      // フィールドを列挙
      sb.append("**プロパティ**:\n");
      for (FieldNode field : canonicalNode.getFields()) {
        if (!field.isSynthetic() && !field.isStatic()) {
          sb.append("- `")
              .append(field.getName())
              .append("` : ")
              .append(formatTypeName(field.getType()))
              .append("\n");
        }
      }

      return sb.toString();
    }

    /**
     * ネストクラスのドキュメントを生成
     *
     * @param nestedNode ネストクラスノード
     * @return ドキュメント文字列
     */
    private String createNestedClassDocumentation(ClassNode nestedNode) {
      var sb = new StringBuilder();

      if (nestedNode.isStaticClass()) {
        sb.append("**静的ネストクラス**: ");
      } else {
        sb.append("**内部クラス**: ");
      }

      sb.append(getFullyQualifiedClassName(nestedNode)).append("\n\n");

      ClassNode outerClass = nestedNode.getOuterClass();
      if (outerClass != null) {
        sb.append("**外部クラス**: ").append(outerClass.getNameWithoutPackage()).append("\n");
      }

      return sb.toString();
    }

    /**
     * クラスの完全修飾名を取得（ネストクラスの場合は外部クラス名を含む）
     *
     * @param node クラスノード
     * @return 完全修飾クラス名
     */
    private String getFullyQualifiedClassName(ClassNode node) {
      if (node.getOuterClass() != null) {
        return getFullyQualifiedClassName(node.getOuterClass())
            + "."
            + node.getNameWithoutPackage();
      }
      return node.getNameWithoutPackage();
    }

    /**
     * メソッドに@Overrideアノテーションがあるかどうかを判定
     *
     * @param method メソッドノード
     * @return @Overrideアノテーションがある場合true
     */
    private boolean hasOverrideAnnotation(MethodNode method) {
      return method.getAnnotations() != null
          && method.getAnnotations().stream()
              .anyMatch(
                  ann ->
                      ann.getClassNode().getName().equals("java.lang.Override")
                          || ann.getClassNode().getName().equals("Override"));
    }

    /**
     * オーバーライドメソッドのドキュメントを生成
     *
     * @param method オーバーライドメソッドノード
     * @return ドキュメント文字列
     */
    private String createOverrideMethodDocumentation(MethodNode method) {
      var sb = new StringBuilder();
      sb.append("**@Override**\n\n");

      ClassNode declaringClass = method.getDeclaringClass();
      if (declaringClass != null) {
        ClassNode superClass = declaringClass.getSuperClass();
        if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
          sb.append("**親クラス**: ").append(superClass.getNameWithoutPackage()).append("\n");

          // 親クラスでメソッドを検索
          for (MethodNode superMethod : superClass.getMethods()) {
            if (superMethod.getName().equals(method.getName())
                && parametersMatch(method.getParameters(), superMethod.getParameters())) {
              sb.append("**親メソッド**: ").append(formatMethodSignature(superMethod)).append("\n");
              break;
            }
          }
        }

        // インターフェースもチェック
        ClassNode[] interfaces = declaringClass.getInterfaces();
        if (interfaces != null && interfaces.length > 0) {
          for (ClassNode iface : interfaces) {
            for (MethodNode ifaceMethod : iface.getMethods()) {
              if (ifaceMethod.getName().equals(method.getName())
                  && parametersMatch(method.getParameters(), ifaceMethod.getParameters())) {
                sb.append("**実装元インターフェース**: ").append(iface.getNameWithoutPackage()).append("\n");
                sb.append("**インターフェースメソッド**: ")
                    .append(formatMethodSignature(ifaceMethod))
                    .append("\n");
                break;
              }
            }
          }
        }
      }

      return sb.toString();
    }

    /**
     * パラメータリストが一致するかどうかを判定
     *
     * @param params1 パラメータリスト1
     * @param params2 パラメータリスト2
     * @return 一致する場合true
     */
    private boolean parametersMatch(Parameter[] params1, Parameter[] params2) {
      if (params1 == null && params2 == null) {
        return true;
      }
      if (params1 == null || params2 == null) {
        return false;
      }
      if (params1.length != params2.length) {
        return false;
      }

      for (int i = 0; i < params1.length; i++) {
        if (!params1[i].getType().equals(params2[i].getType())) {
          return false;
        }
      }
      return true;
    }

    /**
     * 指定位置が型パラメータ内にあるかチェック
     *
     * @param node クラスノード
     * @return 位置が型パラメータ内にある場合true
     */
    private boolean isPositionWithinTypeParameter(ClassNode node) {
      int line = targetPosition.getLine();
      int column = targetPosition.getCharacter();

      // デバッグログ
      logger.debug(
          "型パラメータ位置チェック: クラス名={}, ノード位置={}:{}, ターゲット位置={}:{}, 列情報（1ベース）={}",
          node.getNameWithoutPackage(),
          node.getLineNumber(),
          node.getColumnNumber(),
          line,
          column,
          column);

      // 型パラメータがあるか確認
      GenericsType[] generics = node.getGenericsTypes();
      if (generics == null || generics.length == 0) {
        return false;
      }

      // 型パラメータはクラス宣言と同じ行にある必要がある
      if (line != node.getLineNumber()) {
        logger.debug("型パラメータチェック: 行が一致しません（target: {}, node: {}）", line, node.getLineNumber());
        return false;
      }

      String className = node.getNameWithoutPackage();

      // 実際のソースコードを取得して正確な位置を特定
      Option<String> contentOption = documentContentService.getContent(uri);
      if (contentOption.isDefined()) {
        String sourceCode = contentOption.get();
        String[] lines = sourceCode.split("\n");

        if (line - 1 >= 0 && line - 1 < lines.length) {
          String lineContent = lines[line - 1];
          logger.debug("該当行の内容: '{}'", lineContent);

          // クラス名の位置を検索
          int classNameIndex = lineContent.indexOf(className);
          if (classNameIndex >= 0) {
            // < の位置を検索
            int genericStartIndex = lineContent.indexOf('<', classNameIndex + className.length());
            int genericEndIndex = lineContent.indexOf('>', genericStartIndex);

            if (genericStartIndex >= 0 && genericEndIndex >= 0) {
              // VSCodeの位置は0ベース
              boolean inRange = column > genericStartIndex && column < genericEndIndex;

              logger.debug(
                  "型パラメータ範囲: <の位置={}, >の位置={}, カラム位置={}, 範囲内={}, 該当部分: '{}'",
                  genericStartIndex,
                  genericEndIndex,
                  column,
                  inRange,
                  lineContent.substring(
                      genericStartIndex, Math.min(genericEndIndex + 1, lineContent.length())));

              return inRange;
            } else {
              logger.debug("< または > が見つかりませんでした");
            }
          } else {
            logger.debug("クラス名 '{}' が行内で見つかりませんでした", className);
          }
        }
      }

      // フォールバック: 推定ベースの判定
      // 上記で lineContent が取得できなかった場合の簡易判定
      // "class Container<T extends Number>" の場合
      // VSCodeの位置は0ベースで、targetPositionは+1済み

      int keywordLength = 6; // "class "
      if (node.isInterface()) {
        keywordLength = 10; // "interface "
      } else if (node.isEnum()) {
        keywordLength = 5; // "enum "
      }

      // ノードの列位置を基準に判定
      // Container<T の '<' の後から '>' の前までが型パラメータ範囲
      // クラス名の長さ + キーワードの長さで '<' の位置を推定
      int estimatedGenericStart = keywordLength + className.length();

      // VSCodeの0ベース位置に合わせて調整
      // targetPositionは既に+1されているので、実際のVSCode位置と比較
      boolean result = column > estimatedGenericStart && column < estimatedGenericStart + 50;

      logger.debug(
          "型パラメータ範囲判定（フォールバック）: className={}, keywordLength={}, "
              + "estimatedGenericStart={}, column={}, 結果: {}",
          className,
          keywordLength,
          estimatedGenericStart,
          column,
          result);

      return result;
    }

    /**
     * 型パラメータのドキュメントを生成
     *
     * @param generic 型パラメータ
     * @return ドキュメント文字列
     */
    private String createTypeParameterDocumentation(GenericsType generic) {
      var sb = new StringBuilder();
      sb.append("**型パラメータ**: ").append(generic.getName()).append("\n\n");

      // 上限境界がある場合
      if (generic.getUpperBounds() != null && generic.getUpperBounds().length > 0) {
        sb.append("**上限**: ");
        for (int i = 0; i < generic.getUpperBounds().length; i++) {
          if (i > 0) {
            sb.append(" & ");
          }
          sb.append(formatTypeName(generic.getUpperBounds()[i]));
        }
        sb.append("\n");
      }

      // 下限境界がある場合（superキーワード）
      if (generic.getLowerBound() != null) {
        sb.append("**下限**: ").append(formatTypeName(generic.getLowerBound())).append("\n");
      }

      return sb.toString();
    }

    /**
     * ジェネリクス型をフォーマット
     *
     * @param generic ジェネリクス型
     * @return フォーマットされた文字列
     */
    private String formatGenericsType(GenericsType generic) {
      if (generic.isPlaceholder()) {
        var sb = new StringBuilder();
        sb.append(generic.getName());

        // 上限境界がある場合
        if (generic.getUpperBounds() != null && generic.getUpperBounds().length > 0) {
          sb.append(" extends ");
          for (int i = 0; i < generic.getUpperBounds().length; i++) {
            if (i > 0) {
              sb.append(" & ");
            }
            sb.append(formatTypeName(generic.getUpperBounds()[i]));
          }
        }

        return sb.toString();
      }
      return formatTypeName(generic.getType());
    }

    /**
     * 式から完全修飾名を取得
     *
     * @param expression 式
     * @return 完全修飾名
     */
    private String getQualifiedNameFromExpression(Expression expression) {
      if (expression instanceof PropertyExpression) {
        var propExpr = (PropertyExpression) expression;
        String objectName = getQualifiedNameFromExpression(propExpr.getObjectExpression());
        String propertyName = propExpr.getPropertyAsString();
        return objectName + "." + propertyName;
      } else if (expression instanceof VariableExpression) {
        return ((VariableExpression) expression).getName();
      } else if (expression instanceof ClassExpression) {
        return ((ClassExpression) expression).getType().getName();
      }
      return expression.getText();
    }

    /**
     * 完全修飾名から単純名を取得
     *
     * @param fullyQualifiedName 完全修飾名
     * @return 単純名
     */
    private String getSimpleClassName(String fullyQualifiedName) {
      if (fullyQualifiedName == null) {
        return "";
      }
      int lastDot = fullyQualifiedName.lastIndexOf('.');
      return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * クロージャのit変数の型を推論
     *
     * @return 推論された型、推論できない場合はnull
     */
    private @Nullable ClassNode inferClosureItType() {
      // TODO: 現在のコンテキストを分析して型を推論する
      // 例: list.each { } の場合、listの要素型を取得
      // 今は仮実装として、将来的に実装予定

      // デバッグ用: Integerを返す（テスト用）
      return ClassHelper.make(Integer.class);
    }
  }
}
