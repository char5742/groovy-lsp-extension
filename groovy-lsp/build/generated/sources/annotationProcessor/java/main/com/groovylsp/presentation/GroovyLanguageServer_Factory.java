package com.groovylsp.presentation;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class GroovyLanguageServer_Factory implements Factory<GroovyLanguageServer> {
  private final Provider<GroovyTextDocumentService> textDocumentServiceProvider;

  private final Provider<GroovyWorkspaceService> workspaceServiceProvider;

  public GroovyLanguageServer_Factory(
      Provider<GroovyTextDocumentService> textDocumentServiceProvider,
      Provider<GroovyWorkspaceService> workspaceServiceProvider) {
    this.textDocumentServiceProvider = textDocumentServiceProvider;
    this.workspaceServiceProvider = workspaceServiceProvider;
  }

  @Override
  public GroovyLanguageServer get() {
    return newInstance(textDocumentServiceProvider.get(), workspaceServiceProvider.get());
  }

  public static GroovyLanguageServer_Factory create(
      Provider<GroovyTextDocumentService> textDocumentServiceProvider,
      Provider<GroovyWorkspaceService> workspaceServiceProvider) {
    return new GroovyLanguageServer_Factory(textDocumentServiceProvider, workspaceServiceProvider);
  }

  public static GroovyLanguageServer newInstance(GroovyTextDocumentService textDocumentService,
      GroovyWorkspaceService workspaceService) {
    return new GroovyLanguageServer(textDocumentService, workspaceService);
  }
}
