package com.groovylsp.infrastructure.di;

import com.groovylsp.application.usecase.TextDocumentSyncUseCase;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.infrastructure.repository.InMemoryTextDocumentRepository;
import com.groovylsp.presentation.server.GroovyTextDocumentService;
import com.groovylsp.presentation.server.GroovyWorkspaceService;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** Dagger module for providing server dependencies. */
@Module
public class ServerModule {

  @Provides
  @Singleton
  public TextDocumentRepository provideTextDocumentRepository() {
    return new InMemoryTextDocumentRepository();
  }

  @Provides
  @Singleton
  public GroovyTextDocumentService provideTextDocumentService(TextDocumentSyncUseCase syncUseCase) {
    return new GroovyTextDocumentService(syncUseCase);
  }

  @Provides
  @Singleton
  public GroovyWorkspaceService provideWorkspaceService() {
    return new GroovyWorkspaceService();
  }
}
