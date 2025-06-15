package com.groovylsp.infrastructure.di;

import com.groovylsp.presentation.server.GroovyLanguageServer;
import dagger.Component;
import javax.inject.Singleton;

/** Language Server用のDaggerコンポーネント。 */
@Singleton
@Component(modules = {ServerModule.class})
public interface ServerComponent {

  /** すべての依存関係が注入されたGroovyLanguageServerの新しいインスタンスを作成。 */
  GroovyLanguageServer groovyLanguageServer();
}
