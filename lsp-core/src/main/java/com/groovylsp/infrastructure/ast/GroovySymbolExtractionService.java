package com.groovylsp.infrastructure.ast;

import com.groovylsp.domain.model.Symbol;
import com.groovylsp.domain.service.SymbolExtractionService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
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

  @Inject
  public GroovySymbolExtractionService(GroovyAstParser parser) {
    this.parser = parser;
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
                    Symbol classSymbol = extractClassSymbol(classNode);
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
   * @return クラスのシンボル情報
   */
  private Symbol extractClassSymbol(ClassNode classNode) {
    List<Symbol> children = new ArrayList<>();

    // フィールドを抽出
    for (FieldNode field : classNode.getFields()) {
      if (!field.isSynthetic() && !field.getName().startsWith("$")) {
        children.add(extractFieldSymbol(field));
      }
    }

    // プロパティを抽出
    for (PropertyNode property : classNode.getProperties()) {
      if (!property.isSynthetic()) {
        children.add(extractPropertySymbol(property));
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
        && !methodName.equals("<init>")
        && !methodName.equals("<clinit>");
  }

  /**
   * フィールドノードからシンボル情報を抽出
   *
   * @param field フィールドノード
   * @return フィールドのシンボル情報
   */
  private Symbol extractFieldSymbol(FieldNode field) {
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

    String detail = formatTypeDetail(field.getType());

    return Symbol.create(field.getName(), SymbolKind.Field, range, selectionRange, detail);
  }

  /**
   * プロパティノードからシンボル情報を抽出
   *
   * @param property プロパティノード
   * @return プロパティのシンボル情報
   */
  private Symbol extractPropertySymbol(PropertyNode property) {
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

    String detail = formatTypeDetail(property.getType());

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
}
