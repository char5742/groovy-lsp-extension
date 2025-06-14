package com.groovylsp.infrastructure.di;

import com.groovylsp.presentation.server.GroovyLanguageServer;
import dagger.Component;
import javax.inject.Singleton;

/** Dagger component for the language server. */
@Singleton
@Component(modules = {ServerModule.class})
public interface ServerComponent {

  /** Creates a new instance of the GroovyLanguageServer with all dependencies injected. */
  GroovyLanguageServer groovyLanguageServer();
}
