package com.groovylsp.domain.service;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.ClassInfo;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.FieldInfo;
import com.groovylsp.domain.model.MethodInfo;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * AST解析サービス
 *
 * <p>GroovyAstParserの結果をドメインモデルに変換します。
 */
@NullMarked
@Singleton
public class AstAnalysisService {

  private final GroovyAstParser parser;

  @Inject
  public AstAnalysisService(GroovyAstParser parser) {
    this.parser = parser;
  }

  /**
   * ソースコードを解析してAST情報を取得
   *
   * @param uri ドキュメントURI
   * @param sourceCode ソースコード
   * @return AST情報またはエラー
   */
  public Either<String, AstInfo> analyze(String uri, String sourceCode) {
    // ファイル名をURIから抽出
    String fileName = extractFileName(uri);

    return parser
        .parse(fileName, sourceCode)
        .map(parseResult -> convertToAstInfo(uri, parseResult))
        .mapLeft(error -> error.message());
  }

  /** URIからファイル名を抽出 */
  private String extractFileName(String uri) {
    if (uri.contains("/")) {
      return uri.substring(uri.lastIndexOf("/") + 1);
    }
    return uri;
  }

  /** ParseResultをAstInfoに変換 */
  private AstInfo convertToAstInfo(String uri, GroovyAstParser.ParseResult parseResult) {
    ModuleNode moduleNode = parseResult.moduleNode();

    // パッケージ名を取得（エラー時はmoduleNodeがnullの場合がある）
    String packageName = "";
    if (moduleNode != null && moduleNode.getPackageName() != null) {
      packageName = moduleNode.getPackageName();
      // パッケージ名の末尾のドットを削除
      if (packageName.endsWith(".")) {
        packageName = packageName.substring(0, packageName.length() - 1);
      }
    }

    // インポート文を取得
    List<String> imports = new ArrayList<>();
    if (moduleNode != null && moduleNode.getImports() != null) {
      imports =
          moduleNode.getImports().stream()
              .map(importNode -> importNode.getClassName())
              .collect(Collectors.toList());
    }

    // クラス情報を変換
    List<ClassInfo> classes = new ArrayList<>();
    for (ClassNode classNode : parseResult.getClasses()) {
      ClassInfo classInfo = convertClassNode(classNode);
      if (classInfo != null) {
        classes.add(classInfo);
      }
    }

    // 構文エラーを診断アイテムに変換
    List<DiagnosticItem> syntaxErrors =
        parseResult.diagnostics().stream()
            .filter(diag -> diag.severity() == GroovyAstParser.ParseDiagnostic.Severity.ERROR)
            .map(this::convertToDiagnosticItem)
            .collect(Collectors.toList());

    return new AstInfo(uri, classes, syntaxErrors, packageName, imports);
  }

  /** ClassNodeをClassInfoに変換 */
  private @Nullable ClassInfo convertClassNode(ClassNode classNode) {
    if (classNode == null) {
      return null;
    }

    // クラスの種別を判定
    ClassInfo.ClassType type = determineClassType(classNode);

    // 位置情報を取得
    var position =
        new ClassInfo.Position(
            classNode.getLineNumber(),
            classNode.getColumnNumber(),
            classNode.getLastLineNumber(),
            classNode.getLastColumnNumber());

    // メソッド情報を変換
    List<MethodInfo> methods = new ArrayList<>();
    for (MethodNode methodNode : classNode.getMethods()) {
      MethodInfo methodInfo = convertMethodNode(methodNode);
      if (methodInfo != null) {
        methods.add(methodInfo);
      }
    }

    // フィールド情報を変換
    List<FieldInfo> fields = new ArrayList<>();
    for (FieldNode fieldNode : classNode.getFields()) {
      FieldInfo fieldInfo = convertFieldNode(fieldNode);
      if (fieldInfo != null) {
        fields.add(fieldInfo);
      }
    }

    // スーパータイプを取得
    List<String> superTypes = new ArrayList<>();
    if (classNode.getSuperClass() != null
        && !classNode.getSuperClass().getName().equals("java.lang.Object")) {
      superTypes.add(classNode.getSuperClass().getName());
    }

    // インターフェースを取得
    List<String> interfaces =
        Arrays.stream(classNode.getInterfaces())
            .map(ClassNode::getName)
            .collect(Collectors.toList());

    return new ClassInfo(
        classNode.getName(),
        classNode.getName(), // 完全修飾名は後で実装
        type,
        position,
        methods,
        fields,
        superTypes,
        interfaces,
        classNode.getModifiers());
  }

  /** クラスの種別を判定 */
  private ClassInfo.ClassType determineClassType(ClassNode classNode) {
    if (classNode.isInterface()) {
      return ClassInfo.ClassType.INTERFACE;
    } else if (classNode.isEnum()) {
      return ClassInfo.ClassType.ENUM;
    } else if (classNode.isAnnotationDefinition()) {
      return ClassInfo.ClassType.ANNOTATION;
    } else if (classNode.getName().endsWith("$Trait$Helper")) {
      return ClassInfo.ClassType.TRAIT;
    } else if (classNode.isScript()) {
      return ClassInfo.ClassType.SCRIPT;
    } else {
      return ClassInfo.ClassType.CLASS;
    }
  }

  /** MethodNodeをMethodInfoに変換 */
  private @Nullable MethodInfo convertMethodNode(MethodNode methodNode) {
    if (methodNode == null) {
      return null;
    }

    // 位置情報を取得
    var position =
        new ClassInfo.Position(
            methodNode.getLineNumber(),
            methodNode.getColumnNumber(),
            methodNode.getLastLineNumber(),
            methodNode.getLastColumnNumber());

    // パラメータ情報を変換
    List<MethodInfo.ParameterInfo> parameters = new ArrayList<>();
    for (Parameter param : methodNode.getParameters()) {
      parameters.add(
          new MethodInfo.ParameterInfo(
              param.getName(),
              param.getType().getName(),
              param.hasInitialExpression() ? "has default" : null, // 実際の値は後で実装
              param.getType().isArray()));
    }

    // 戻り値の型を取得
    String returnType = methodNode.getReturnType().getName();

    return new MethodInfo(
        methodNode.getName(),
        returnType,
        parameters,
        position,
        methodNode.getModifiers(),
        null // ドキュメントは後で実装
        );
  }

  /** FieldNodeをFieldInfoに変換 */
  private @Nullable FieldInfo convertFieldNode(FieldNode fieldNode) {
    if (fieldNode == null) {
      return null;
    }

    // 位置情報を取得
    var position =
        new ClassInfo.Position(
            fieldNode.getLineNumber(),
            fieldNode.getColumnNumber(),
            fieldNode.getLastLineNumber(),
            fieldNode.getLastColumnNumber());

    return new FieldInfo(
        fieldNode.getName(),
        fieldNode.getType().getName(),
        position,
        fieldNode.getModifiers(),
        fieldNode.hasInitialExpression() ? "has initial value" : null, // 実際の値は後で実装
        null // ドキュメントは後で実装
        );
  }

  /** ParseDiagnosticをDiagnosticItemに変換 */
  private DiagnosticItem convertToDiagnosticItem(GroovyAstParser.ParseDiagnostic parseDiag) {
    return new DiagnosticItem(
        new DiagnosticItem.DocumentPosition(
            parseDiag.start().line() - 1, parseDiag.start().character() - 1),
        new DiagnosticItem.DocumentPosition(
            parseDiag.end().line() - 1, parseDiag.end().character() - 1),
        convertSeverity(parseDiag.severity()),
        parseDiag.message(),
        "groovy-syntax");
  }

  /** 診断の重要度を変換 */
  private DiagnosticItem.DiagnosticSeverity convertSeverity(
      GroovyAstParser.ParseDiagnostic.Severity severity) {
    return switch (severity) {
      case ERROR -> DiagnosticItem.DiagnosticSeverity.ERROR;
      case WARNING -> DiagnosticItem.DiagnosticSeverity.WARNING;
      case INFO -> DiagnosticItem.DiagnosticSeverity.INFORMATION;
    };
  }
}
