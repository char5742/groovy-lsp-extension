package com.groovylsp.infrastructure.di;

import com.groovylsp.presentation.GroovyTextDocumentService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ServerModule_ProvideTextDocumentServiceFactory implements Factory<GroovyTextDocumentService> {
  private final ServerModule module;

  public ServerModule_ProvideTextDocumentServiceFactory(ServerModule module) {
    this.module = module;
  }

  @Override
  public GroovyTextDocumentService get() {
    return provideTextDocumentService(module);
  }

  public static ServerModule_ProvideTextDocumentServiceFactory create(ServerModule module) {
    return new ServerModule_ProvideTextDocumentServiceFactory(module);
  }

  public static GroovyTextDocumentService provideTextDocumentService(ServerModule instance) {
    return Preconditions.checkNotNullFromProvides(instance.provideTextDocumentService());
  }
}
