package com.groovylsp.infrastructure.di;

import com.groovylsp.application.usecase.DiagnosticUseCase;
import com.groovylsp.application.usecase.DocumentSymbolUseCase;
import com.groovylsp.application.usecase.TextDocumentSyncUseCase;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.AstAnalysisService;
import com.groovylsp.domain.service.BracketValidationService;
import com.groovylsp.domain.service.LineCountService;
import com.groovylsp.domain.service.SymbolExtractionService;
import com.groovylsp.infrastructure.ast.GroovySymbolExtractionService;
import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
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
  public DocumentSymbolUseCase provideDocumentSymbolUseCase(
      SymbolExtractionService symbolExtractionService, TextDocumentRepository repository) {
    return new DocumentSymbolUseCase(symbolExtractionService, repository);
  }

  @Provides
  @Singleton
  public GroovyTextDocumentService provideTextDocumentService(
      TextDocumentSyncUseCase syncUseCase,
      DiagnosticUseCase diagnosticUseCase,
      DocumentSymbolUseCase documentSymbolUseCase) {
    return new GroovyTextDocumentService(syncUseCase, diagnosticUseCase, documentSymbolUseCase);
  }

  @Provides
  @Singleton
  public GroovyWorkspaceService provideWorkspaceService() {
    return new GroovyWorkspaceService();
  }
}
