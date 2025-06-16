package com.groovylsp.infrastructure.symbol;

import com.groovylsp.domain.model.AstInfo;
import com.groovylsp.domain.model.ClassInfo;
import com.groovylsp.domain.model.MethodInfo;
import com.groovylsp.domain.model.Scope;
import com.groovylsp.domain.model.SymbolDefinition;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.service.SymbolTableBuilderService;
import io.vavr.control.Either;
import io.vavr.control.Try;
import javax.inject.Inject;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/** Groovy AST情報からシンボルテーブルとスコープを構築するサービスの実装 */
public class GroovySymbolTableBuilderService implements SymbolTableBuilderService {

  @Inject
  public GroovySymbolTableBuilderService() {}

  @Override
  public Either<String, Void> buildSymbolTable(
      AstInfo astInfo, String uri, SymbolTable symbolTable) {
    return Try.<Void>of(
            () -> {
              // ファイル内の既存シンボルをクリア
              symbolTable.clearFile(uri);

              // クラス定義をシンボルテーブルに追加
              astInfo
                  .classes()
                  .forEach(
                      classInfo -> {
                        // クラス自体のシンボル定義を作成
                        Range classRange = createRange(classInfo.position());
                        Range classSelectionRange = createSelectionRange(classInfo.position());
                        SymbolDefinition classDef =
                            SymbolDefinition.forClass(
                                classInfo, uri, classRange, classSelectionRange);
                        symbolTable.addSymbol(classDef);

                        // メソッドのシンボル定義を作成
                        classInfo
                            .methods()
                            .forEach(
                                methodInfo -> {
                                  Range methodRange = createRange(methodInfo.position());
                                  Range methodSelectionRange =
                                      createSelectionRange(methodInfo.position());
                                  SymbolDefinition methodDef =
                                      SymbolDefinition.forMethod(
                                          methodInfo,
                                          classInfo.qualifiedName(),
                                          uri,
                                          methodRange,
                                          methodSelectionRange);
                                  symbolTable.addSymbol(methodDef);
                                });

                        // フィールドのシンボル定義を作成
                        classInfo
                            .fields()
                            .forEach(
                                fieldInfo -> {
                                  Range fieldRange = createRange(fieldInfo.position());
                                  Range fieldSelectionRange =
                                      createSelectionRange(fieldInfo.position());
                                  SymbolDefinition fieldDef =
                                      SymbolDefinition.forField(
                                          fieldInfo,
                                          classInfo.qualifiedName(),
                                          uri,
                                          fieldRange,
                                          fieldSelectionRange);
                                  symbolTable.addSymbol(fieldDef);
                                });
                      });

              return null;
            })
        .toEither()
        .mapLeft(Throwable::getMessage);
  }

  @Override
  public Either<String, Scope> buildScope(AstInfo astInfo, String uri) {
    return Try.of(
            () -> {
              // ファイル全体のルートスコープを作成
              var fileRange = new Range(new Position(0, 0), new Position(Integer.MAX_VALUE, 0));
              var rootScope = new Scope(Scope.ScopeType.GLOBAL, null, fileRange, null);

              // 各クラスのスコープを構築
              astInfo
                  .classes()
                  .forEach(
                      classInfo -> {
                        buildClassScope(classInfo, rootScope, uri);
                      });

              return rootScope;
            })
        .toEither()
        .mapLeft(Throwable::getMessage);
  }

  /** クラスのスコープを構築 */
  private void buildClassScope(ClassInfo classInfo, Scope parentScope, String uri) {
    // クラススコープを作成
    Range classRange = createRange(classInfo.position());
    var classScope =
        new Scope(Scope.ScopeType.CLASS, parentScope, classRange, classInfo.qualifiedName());

    // クラス自体をスコープに追加
    Range classSelectionRange = createSelectionRange(classInfo.position());
    SymbolDefinition classDef =
        SymbolDefinition.forClass(classInfo, uri, classRange, classSelectionRange);
    parentScope.addSymbol(classDef);

    // フィールドをクラススコープに追加
    classInfo
        .fields()
        .forEach(
            fieldInfo -> {
              Range fieldRange = createRange(fieldInfo.position());
              Range fieldSelectionRange = createSelectionRange(fieldInfo.position());
              SymbolDefinition fieldDef =
                  SymbolDefinition.forField(
                      fieldInfo, classInfo.qualifiedName(), uri, fieldRange, fieldSelectionRange);
              classScope.addSymbol(fieldDef);
            });

    // メソッドのスコープを構築
    classInfo
        .methods()
        .forEach(
            methodInfo -> {
              buildMethodScope(methodInfo, classScope, classInfo.qualifiedName(), uri);
            });
  }

  /** メソッドのスコープを構築 */
  private void buildMethodScope(
      MethodInfo methodInfo, Scope classScope, String qualifiedClassName, String uri) {
    // メソッドスコープを作成
    Range methodRange = createRange(methodInfo.position());
    var methodScope = new Scope(Scope.ScopeType.METHOD, classScope, methodRange, methodInfo.name());

    // メソッド自体をクラススコープに追加
    Range methodSelectionRange = createSelectionRange(methodInfo.position());
    SymbolDefinition methodDef =
        SymbolDefinition.forMethod(
            methodInfo, qualifiedClassName, uri, methodRange, methodSelectionRange);
    classScope.addSymbol(methodDef);

    // パラメータをメソッドスコープに追加
    methodInfo
        .parameters()
        .forEach(
            param -> {
              // パラメータの位置情報は現時点では利用できないため、メソッドの範囲を使用
              var paramDef =
                  new SymbolDefinition(
                      param.name(),
                      qualifiedClassName + "." + methodInfo.name() + "." + param.name(),
                      org.eclipse.lsp4j.SymbolKind.Variable,
                      uri,
                      methodRange,
                      methodRange,
                      qualifiedClassName + "." + methodInfo.name(),
                      SymbolDefinition.DefinitionType.PARAMETER);
              methodScope.addSymbol(paramDef);
            });
  }

  /** Position情報からRangeを作成 */
  private Range createRange(ClassInfo.Position position) {
    return new Range(
        new Position(position.startLine() - 1, position.startColumn() - 1),
        new Position(position.endLine() - 1, position.endColumn() - 1));
  }

  /** Position情報から選択範囲（名前部分）のRangeを作成 現時点では全体範囲と同じものを返す */
  private Range createSelectionRange(ClassInfo.Position position) {
    // TODO: より正確な名前部分の範囲を計算する
    return createRange(position);
  }
}
