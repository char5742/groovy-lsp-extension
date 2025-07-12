package com.groovylsp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovylsp.application.usecase.HoverUseCase;
import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.infrastructure.ast.GroovyTypeInfoService;
import com.groovylsp.infrastructure.documentation.GroovyDocumentationService;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.testing.FastTest;
import java.net.URI;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ドキュメンテーション機能の統合テスト
 */
@FastTest
class DocumentationIntegrationTest {

  private HoverUseCase hoverUseCase;
  private TextDocumentRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryTextDocumentRepository();
    var parser = new GroovyAstParser();
    var symbolTable = new SymbolTable();
    var scopeManager = new ScopeManager();
    var documentContentService = new DocumentContentService(repository);
    var astAnalysisService = new AstAnalysisService(parser);
    var documentationService = new GroovyDocumentationService();
    
    TypeInfoService typeInfoService = new GroovyTypeInfoService(
        parser, symbolTable, scopeManager, documentContentService, astAnalysisService, documentationService);
    
    hoverUseCase = new HoverUseCase(repository, typeInfoService);
  }

  @Test
  void ドキュメント付きクラスのホバー情報を取得() {
    // given
    String groovyCode = """
        package com.example
        
        /**
         * サンプルクラスです。
         * このクラスは何かを処理します。
         *
         * @since 1.0
         * @author 開発者
         */
        class SampleClass {
            
            /**
             * 名前フィールドです。
             */
            String name
            
            /**
             * データを処理するメソッドです。
             * @param input 入力データ
             * @return 処理結果
             * @throws IllegalArgumentException 引数が不正な場合
             */
            def process(input) {
                if (input == null) {
                    throw new IllegalArgumentException("Input cannot be null")
                }
                return input.toString().toUpperCase()
            }
        }
        """;

    String uri = "file:///test/SampleClass.groovy";
    repository.save(new TextDocument(URI.create(uri), "groovy", 1, groovyCode));

    // when - クラス名にホバー
    var classHoverParams = new HoverParams();
    classHoverParams.setTextDocument(new TextDocumentIdentifier(uri));
    classHoverParams.setPosition(new Position(9, 10)); // "SampleClass" の位置

    var classHoverResult = hoverUseCase.getHover(classHoverParams);

    // then
    assertThat(classHoverResult.isRight()).isTrue();
    var classHover = classHoverResult.get();
    if (classHover != null && classHover.getContents().isRight()) {
      String content = classHover.getContents().getRight().getValue();
      assertThat(content).contains("SampleClass");
      // ドキュメントが表示されることを確認
      assertThat(content).contains("サンプルクラスです");
    }

    // when - メソッド名にホバー
    var methodHoverParams = new HoverParams();
    methodHoverParams.setTextDocument(new TextDocumentIdentifier(uri));
    methodHoverParams.setPosition(new Position(21, 16)); // "process" の位置

    var methodHoverResult = hoverUseCase.getHover(methodHoverParams);

    // then
    assertThat(methodHoverResult.isRight()).isTrue();
    var methodHover = methodHoverResult.get();
    if (methodHover != null && methodHover.getContents().isRight()) {
      String content = methodHover.getContents().getRight().getValue();
      assertThat(content).contains("process");
      // ドキュメントが表示されることを確認
      assertThat(content).contains("データを処理する");
    }
  }

  @Test
  void ドキュメントなしの要素のホバー情報() {
    // given
    String groovyCode = """
        class SimpleClass {
            String field
            
            def method() {
                return "hello"
            }
        }
        """;

    String uri = "file:///test/SimpleClass.groovy";
    repository.save(new TextDocument(URI.create(uri), "groovy", 1, groovyCode));

    // when
    var hoverParams = new HoverParams();
    hoverParams.setTextDocument(new TextDocumentIdentifier(uri));
    hoverParams.setPosition(new Position(0, 6)); // "SimpleClass" の位置

    var hoverResult = hoverUseCase.getHover(hoverParams);

    // then
    assertThat(hoverResult.isRight()).isTrue();
    var hover = hoverResult.get();
    if (hover != null && hover.getContents().isRight()) {
      String content = hover.getContents().getRight().getValue();
      assertThat(content).contains("SimpleClass");
      // ドキュメントがない場合でも基本情報は表示される
    }
  }
}