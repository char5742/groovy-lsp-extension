package com.groovylsp;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.presentation.GroovyLanguageServer;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the Groovy Language Server. */
public final class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private Main() {
    // Utility class
  }

  public static void main(String[] args) {
    LOGGER.info("Starting Groovy Language Server...");

    Either<Throwable, Void> result =
        Try.<Void>of(
                () -> {
                  // Create the language server instance using Dagger
                  ServerComponent serverComponent = DaggerServerComponent.create();
                  GroovyLanguageServer server = serverComponent.groovyLanguageServer();

                  // Create the launcher for the language server
                  Launcher<LanguageClient> launcher =
                      LSPLauncher.createServerLauncher(server, System.in, System.out);

                  // Connect the language client
                  LanguageClient client = launcher.getRemoteProxy();
                  server.connect(client);

                  // Start listening for messages
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
