package com.groovylsp.infrastructure.di;

import com.groovylsp.presentation.GroovyLanguageServer;
import com.groovylsp.presentation.GroovyLanguageServer_Factory;
import com.groovylsp.presentation.GroovyTextDocumentService;
import com.groovylsp.presentation.GroovyWorkspaceService;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import javax.annotation.processing.Generated;

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
public final class DaggerServerComponent {
  private DaggerServerComponent() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ServerComponent create() {
    return new Builder().build();
  }

  public static final class Builder {
    private ServerModule serverModule;

    private Builder() {
    }

    public Builder serverModule(ServerModule serverModule) {
      this.serverModule = Preconditions.checkNotNull(serverModule);
      return this;
    }

    public ServerComponent build() {
      if (serverModule == null) {
        this.serverModule = new ServerModule();
      }
      return new ServerComponentImpl(serverModule);
    }
  }

  private static final class ServerComponentImpl implements ServerComponent {
    private final ServerComponentImpl serverComponentImpl = this;

    private Provider<GroovyTextDocumentService> provideTextDocumentServiceProvider;

    private Provider<GroovyWorkspaceService> provideWorkspaceServiceProvider;

    private Provider<GroovyLanguageServer> groovyLanguageServerProvider;

    private ServerComponentImpl(ServerModule serverModuleParam) {

      initialize(serverModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ServerModule serverModuleParam) {
      this.provideTextDocumentServiceProvider = DoubleCheck.provider(ServerModule_ProvideTextDocumentServiceFactory.create(serverModuleParam));
      this.provideWorkspaceServiceProvider = DoubleCheck.provider(ServerModule_ProvideWorkspaceServiceFactory.create(serverModuleParam));
      this.groovyLanguageServerProvider = DoubleCheck.provider(GroovyLanguageServer_Factory.create(provideTextDocumentServiceProvider, provideWorkspaceServiceProvider));
    }

    @Override
    public GroovyLanguageServer groovyLanguageServer() {
      return groovyLanguageServerProvider.get();
    }
  }
}
