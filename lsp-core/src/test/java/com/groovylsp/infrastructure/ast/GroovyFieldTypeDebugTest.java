package com.groovylsp.infrastructure.ast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.SymbolTableBuilderService;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.infrastructure.symbol.GroovySymbolTableBuilderService;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** フィールドの型情報問題を調査するためのデバッグテスト */
@FastTest
class GroovyFieldTypeDebugTest {

  private GroovyTypeInfoService typeInfoService;
  private AstAnalysisService astAnalysisService;
  private SymbolTableBuilderService symbolTableBuilderService;
  private SymbolTable symbolTable;

  @BeforeEach
  void setUp() {
    var parser = new GroovyAstParser();
    symbolTable = new SymbolTable();
    var scopeManager = new ScopeManager();
    TextDocumentRepository repository = new InMemoryTextDocumentRepository();
    var documentContentService = new DocumentContentService(repository);
    typeInfoService =
        new GroovyTypeInfoService(parser, symbolTable, scopeManager, documentContentService);
    astAnalysisService = new AstAnalysisService(parser);
    symbolTableBuilderService = new GroovySymbolTableBuilderService();
  }

  @Test
  void フィールドの型情報の流れを詳細に追跡() {
    // given
    String content =
        """
        class Person {
            private String name = "John"
            String surname = "Doe"
            int age = 25
            def dynamic = "value"
        }
        """;

    String uri = "test.groovy";

    // Step 1: AST解析
    System.out.println("=== Step 1: AST解析 ===");
    var astInfoResult = astAnalysisService.analyze(uri, content);
    assertTrue(astInfoResult.isRight());
    var astInfo = astInfoResult.get();

    astInfo
        .classes()
        .forEach(
            classInfo -> {
              System.out.println("Class: " + classInfo.name());
              classInfo
                  .fields()
                  .forEach(
                      field -> {
                        System.out.println(
                            "  Field: "
                                + field.name()
                                + " - Type from AST: "
                                + field.type()
                                + " (modifiers: "
                                + field.modifiers()
                                + ")");
                      });
            });

    // Step 2: シンボルテーブル構築
    System.out.println("\n=== Step 2: シンボルテーブル構築 ===");
    var buildResult = symbolTableBuilderService.buildSymbolTable(astInfo, uri, symbolTable);
    assertTrue(buildResult.isRight());

    // シンボルテーブルの内容を確認
    var symbols = symbolTable.getSymbolsInFile(uri);
    symbols.forEach(
        symbol -> {
          System.out.println(
              "Symbol: "
                  + symbol.name()
                  + " - Type: "
                  + symbol.definitionType()
                  + " - Qualified: "
                  + symbol.qualifiedName());
        });

    // Step 3: TypeInfoService経由で型情報取得
    System.out.println("\n=== Step 3: TypeInfoService経由で型情報取得 ===");

    // nameフィールドの型情報を取得
    Either<String, TypeInfoService.TypeInfo> result =
        typeInfoService.getTypeInfoAt(uri, content, new Position(1, 19)); // "name"の位置
    assertTrue(result.isRight());
    var typeInfo = result.get();
    System.out.println("Field 'name' type info:");
    System.out.println("  Name: " + typeInfo.name());
    System.out.println("  Type: " + typeInfo.type());
    System.out.println("  Kind: " + typeInfo.kind());
    System.out.println("  Modifiers: " + typeInfo.modifiers());

    // surnameフィールドの型情報を取得
    result = typeInfoService.getTypeInfoAt(uri, content, new Position(2, 11)); // "surname"の位置
    assertTrue(result.isRight());
    typeInfo = result.get();
    System.out.println("\nField 'surname' type info:");
    System.out.println("  Name: " + typeInfo.name());
    System.out.println("  Type: " + typeInfo.type());
    System.out.println("  Kind: " + typeInfo.kind());
    System.out.println("  Modifiers: " + typeInfo.modifiers());

    // dynamicフィールドの型情報を取得
    result = typeInfoService.getTypeInfoAt(uri, content, new Position(4, 11)); // "dynamic"の位置
    assertTrue(result.isRight());
    typeInfo = result.get();
    System.out.println("\nField 'dynamic' type info:");
    System.out.println("  Name: " + typeInfo.name());
    System.out.println("  Type: " + typeInfo.type());
    System.out.println("  Kind: " + typeInfo.kind());
    System.out.println("  Modifiers: " + typeInfo.modifiers());
  }
}
