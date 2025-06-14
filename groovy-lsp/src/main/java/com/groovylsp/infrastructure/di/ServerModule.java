package com.groovylsp.infrastructure.di;

import com.groovylsp.presentation.GroovyTextDocumentService;
import com.groovylsp.presentation.GroovyWorkspaceService;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/** Dagger module for providing server dependencies. */
@Module
public class ServerModule {

  @Provides
  @Singleton
  public GroovyTextDocumentService provideTextDocumentService() {
    return new GroovyTextDocumentService();
  }

  @Provides
  @Singleton
  public GroovyWorkspaceService provideWorkspaceService() {
    return new GroovyWorkspaceService();
  }
}
