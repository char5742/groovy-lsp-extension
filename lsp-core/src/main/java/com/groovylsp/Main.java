package com.groovylsp;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.presentation.server.GroovyLanguageServer;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Groovy Language Serverのメインエントリーポイント。 */
public final class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private Main() {
    // ユーティリティクラス
  }

  public static void main(String[] args) {
    LOGGER.info("Starting Groovy Language Server...");

    Either<Throwable, @Nullable Void> result =
        Try.<Void>of(
                () -> {
                  // Daggerを使用してLanguage Serverインスタンスを作成
                  ServerComponent serverComponent = DaggerServerComponent.create();
                  GroovyLanguageServer server = serverComponent.groovyLanguageServer();

                  // Language Server用のランチャーを作成
                  Launcher<LanguageClient> launcher =
                      LSPLauncher.createServerLauncher(server, System.in, System.out);

                  // Language Clientを接続
                  LanguageClient client = launcher.getRemoteProxy();
                  server.connect(client);

                  // メッセージのリッスンを開始
                  launcher.startListening().get();
                  return null;
                })
            .toEither();

    result.peekLeft(
        error -> {
          LOGGER.error("Error while listening for messages", error);
          if (error instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
          System.exit(1);
        });
  }
}
