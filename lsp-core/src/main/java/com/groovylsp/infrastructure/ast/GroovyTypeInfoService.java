package com.groovylsp.infrastructure.ast;

import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolDefinition;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
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

  @Inject
  public GroovyTypeInfoService(
      GroovyAstParser parser,
      SymbolTable symbolTable,
      ScopeManager scopeManager,
      DocumentContentService documentContentService) {
    this.parser = parser;
    this.symbolTable = symbolTable;
    this.scopeManager = scopeManager;
    this.documentContentService = documentContentService;
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

              // 指定位置の要素を探索
              var visitor = new TypeInfoVisitor(position, uri);
              for (ClassNode classNode : parseResult.getClasses()) {
                visitor.visitClass(classNode);
              }

              TypeInfo typeInfo = visitor.getFoundTypeInfo();
              if (typeInfo != null) {
                return Either.right(typeInfo);
              }

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

    public TypeInfoVisitor(Position targetPosition, String uri) {
      // LSPの位置は0ベース、Groovyは1ベースなので+1で変換
      this.targetPosition =
          new Position(targetPosition.getLine() + 1, targetPosition.getCharacter() + 1);
      this.uri = uri;
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

      // クラスのすべてのフィールドの型情報を事前に記録
      // （メソッド内からフィールドを参照する際に必要）
      for (FieldNode field : node.getFields()) {
        variableTypes.put(field.getName(), field.getType());
        logger.debug(
            "クラス {} のフィールドを事前記録: {} (型: {})",
            node.getName(),
            field.getName(),
            field.getType().getName());
      }

      // まず子要素（フィールドとメソッド）をチェック
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

      // 子要素で見つからなかった場合、クラス名の位置をチェック
      if (isPositionWithinClassName(node)) {
        foundTypeInfo =
            new TypeInfo(
                node.getName(),
                node.getName(),
                TypeInfo.Kind.CLASS,
                null,
                getModifiersString(node.getModifiers()));
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
          "クラス位置チェック: クラス名={}, ノード位置={}:{}, ターゲット位置={}:{}",
          node.getNameWithoutPackage(),
          node.getLineNumber(),
          node.getColumnNumber(),
          line,
          column);

      // クラス名は "class " キーワードの後にある（6文字分オフセット）
      String className = node.getNameWithoutPackage();
      int classKeywordLength = 6; // "class " の長さ
      return line == node.getLineNumber()
          && column >= node.getColumnNumber() + classKeywordLength
          && column < node.getColumnNumber() + classKeywordLength + className.length();
    }

    @Override
    public void visitField(FieldNode node) {
      if (foundTypeInfo != null) {
        return;
      }

      if (isPositionWithin(node)) {
        foundTypeInfo =
            new TypeInfo(
                node.getName(),
                formatTypeName(node.getType()),
                TypeInfo.Kind.FIELD,
                null,
                getModifiersString(node.getModifiers()));
      }
    }

    @Override
    public void visitMethod(MethodNode node) {
      if (foundTypeInfo != null) {
        return;
      }

      logger.debug(
          "visitMethod: {} (isConstructor: {}) at {}:{}",
          node.getName(),
          node.getName().equals("<init>"),
          node.getLineNumber(),
          node.getColumnNumber());

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
          foundTypeInfo =
              new TypeInfo(
                  node.getName(),
                  formatMethodSignature(node),
                  TypeInfo.Kind.METHOD,
                  null,
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

      // Groovyのメソッドノードの位置は返り値型の後から始まる
      // 返り値型の長さを推定して、メソッド名の実際の位置を計算
      String returnTypeName = node.getReturnType().getNameWithoutPackage();
      int typeAndSpaceLength = returnTypeName.length() + 1; // 型名 + スペース

      // デバッグログ追加
      logger.debug(
          "メソッド名位置計算: 返り値型={}, 型長さ={}, 計算後位置={}",
          returnTypeName,
          typeAndSpaceLength,
          node.getColumnNumber() + typeAndSpaceLength);

      return line == node.getLineNumber()
          && column >= node.getColumnNumber() + typeAndSpaceLength
          && column < node.getColumnNumber() + typeAndSpaceLength + node.getName().length();
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
        if (declaredType != null && !declaredType.getName().equals("java.lang.Object")) {
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
            scopeManager.findSymbolAt(
                uri,
                new Position(targetPosition.getLine() - 1, targetPosition.getCharacter() - 1),
                varName);

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
      logger.debug("メソッド式のタイプ: {}", method != null ? method.getClass().getName() : "null");

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

          // シンボルテーブルから検索
          Option<SymbolDefinition> symbolOption =
              scopeManager.findSymbolAt(
                  uri,
                  new Position(targetPosition.getLine() - 1, targetPosition.getCharacter() - 1),
                  methodName);

          if (symbolOption.isDefined()) {
            foundTypeInfo = createTypeInfoFromSymbol(symbolOption.get());
          } else {
            // メソッド呼び出しとして扱う
            foundTypeInfo = new TypeInfo(methodName, "メソッド呼び出し", TypeInfo.Kind.METHOD, null, null);
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
              scopeManager.findSymbolAt(
                  uri,
                  new Position(targetPosition.getLine() - 1, targetPosition.getCharacter() - 1),
                  methodName);

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

          // フィールドとして記録されているか確認
          ClassNode recordedType = variableTypes.get(propName);
          if (recordedType != null) {
            foundTypeInfo =
                new TypeInfo(
                    propName, formatTypeName(recordedType), TypeInfo.Kind.FIELD, null, null);
          } else {
            // シンボルテーブルから検索
            Option<SymbolDefinition> symbolOption =
                scopeManager.findSymbolAt(
                    uri,
                    new Position(targetPosition.getLine() - 1, targetPosition.getCharacter() - 1),
                    propName);

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
                scopeManager.findSymbolAt(
                    uri,
                    new Position(targetPosition.getLine() - 1, targetPosition.getCharacter() - 1),
                    propName);

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
      if (parameters != null) {
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

      // ジェネリクス型の基本的な処理
      GenericsType[] generics = type.getGenericsTypes();
      if (generics != null && generics.length > 0) {
        var sb = new StringBuilder(type.getNameWithoutPackage());
        sb.append("<");
        for (int i = 0; i < generics.length; i++) {
          if (i > 0) {
            sb.append(", ");
          }
          sb.append(formatTypeName(generics[i].getType()));
        }
        sb.append(">");
        return sb.toString();
      }

      return type.getNameWithoutPackage();
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
      return line == constructor.getLineNumber()
          && column >= constructor.getColumnNumber()
          && column < constructor.getColumnNumber() + className.length();
    }
  }
}
