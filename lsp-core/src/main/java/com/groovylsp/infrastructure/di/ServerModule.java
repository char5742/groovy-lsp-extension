package com.groovylsp.infrastructure.di;

import com.groovylsp.application.usecase.DiagnosticUseCase;
import com.groovylsp.application.usecase.DocumentSymbolUseCase;
import com.groovylsp.application.usecase.HoverUseCase;
import com.groovylsp.application.usecase.TextDocumentSyncUseCase;
import com.groovylsp.domain.model.ScopeManager;
import com.groovylsp.domain.model.SymbolTable;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.DefinitionFinderService;
import com.groovylsp.domain.service.DocumentationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.domain.service.SymbolExtractionService;
import com.groovylsp.domain.service.SymbolTableBuilderService;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.infrastructure.ast.GroovySymbolExtractionService;
import com.groovylsp.infrastructure.ast.GroovyTypeInfoService;
import com.groovylsp.infrastructure.documentation.GroovyDocumentationService;
import com.groovylsp.infrastructure.documentation.SourceDocumentExtractor;
import com.groovylsp.infrastructure.parser.DocumentContentService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.infrastructure.symbol.GroovyDefinitionFinderService;
import com.groovylsp.infrastructure.symbol.GroovySymbolTableBuilderService;
import com.groovylsp.presentation.server.GroovyTextDocumentService;
import com.groovylsp.presentation.server.GroovyWorkspaceService;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** サーバーの依存関係を提供するDaggerモジュール。 */
@Module
public class ServerModule {

  @Provides
  @Singleton
  public TextDocumentRepository provideTextDocumentRepository() {
    return new InMemoryTextDocumentRepository();
  }

  @Provides
  @Singleton
  public LineCountService provideLineCountService() {
    return new LineCountService();
  }

  @Provides
  @Singleton
  public BracketValidationService provideBracketValidationService() {
    return new BracketValidationService();
  }

  @Provides
  @Singleton
  public GroovyAstParser provideGroovyAstParser() {
    return new GroovyAstParser();
  }

  @Provides
  @Singleton
  public AstAnalysisService provideAstAnalysisService(GroovyAstParser parser) {
    return new AstAnalysisService(parser);
  }

  @Provides
  @Singleton
  public SymbolExtractionService provideSymbolExtractionService(GroovyAstParser parser) {
    return new GroovySymbolExtractionService(parser);
  }

  @Provides
  @Singleton
  public DocumentationService provideDocumentationService() {
    // 循環依存を避けるため、DocumentationServiceを先に作成
    return new GroovyDocumentationService();
  }

  @Provides
  @Singleton
  public SourceDocumentExtractor provideSourceDocumentExtractor(DocumentationService documentationService) {
    return new SourceDocumentExtractor(documentationService);
  }

  @Provides
  @Singleton
  public TypeInfoService provideTypeInfoService(
      GroovyAstParser parser,
      SymbolTable symbolTable,
      ScopeManager scopeManager,
      DocumentContentService documentContentService,
      AstAnalysisService astAnalysisService,
      DocumentationService documentationService) {
    return new GroovyTypeInfoService(
        parser, symbolTable, scopeManager, documentContentService, astAnalysisService, documentationService);
  }

  @Provides
  @Singleton
  public DocumentSymbolUseCase provideDocumentSymbolUseCase(
      SymbolExtractionService symbolExtractionService, TextDocumentRepository repository) {
    return new DocumentSymbolUseCase(symbolExtractionService, repository);
  }

  @Provides
  @Singleton
  public HoverUseCase provideHoverUseCase(
      TextDocumentRepository repository, TypeInfoService typeInfoService) {
    return new HoverUseCase(repository, typeInfoService);
  }

  @Provides
  @Singleton
  public GroovyTextDocumentService provideTextDocumentService(
      TextDocumentSyncUseCase syncUseCase,
      DiagnosticUseCase diagnosticUseCase,
      DocumentSymbolUseCase documentSymbolUseCase,
      HoverUseCase hoverUseCase) {
    return new GroovyTextDocumentService(
        syncUseCase, diagnosticUseCase, documentSymbolUseCase, hoverUseCase);
  }

  @Provides
  @Singleton
  public GroovyWorkspaceService provideWorkspaceService() {
    return new GroovyWorkspaceService();
  }

  @Provides
  @Singleton
  public SymbolTable provideSymbolTable() {
    return new SymbolTable();
  }

  @Provides
  @Singleton
  public ScopeManager provideScopeManager() {
    return new ScopeManager();
  }

  @Provides
  @Singleton
  public DocumentContentService provideDocumentContentService(TextDocumentRepository repository) {
    return new DocumentContentService(repository);
  }

  @Provides
  @Singleton
  public SymbolTableBuilderService provideSymbolTableBuilderService() {
    return new GroovySymbolTableBuilderService();
  }

  @Provides
  @Singleton
  public DefinitionFinderService provideDefinitionFinderService(
      SymbolTable symbolTable,
      ScopeManager scopeManager,
      DocumentContentService documentContentService) {
    return new GroovyDefinitionFinderService(symbolTable, scopeManager, documentContentService);
  }
}
