package com.groovylsp.infrastructure.di;

import com.groovylsp.presentation.GroovyWorkspaceService;
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
public final class ServerModule_ProvideWorkspaceServiceFactory implements Factory<GroovyWorkspaceService> {
  private final ServerModule module;

  public ServerModule_ProvideWorkspaceServiceFactory(ServerModule module) {
    this.module = module;
  }

  @Override
  public GroovyWorkspaceService get() {
    return provideWorkspaceService(module);
  }

  public static ServerModule_ProvideWorkspaceServiceFactory create(ServerModule module) {
    return new ServerModule_ProvideWorkspaceServiceFactory(module);
  }

  public static GroovyWorkspaceService provideWorkspaceService(ServerModule instance) {
    return Preconditions.checkNotNullFromProvides(instance.provideWorkspaceService());
  }
}
