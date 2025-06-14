package com.groovylsp;

import com.groovylsp.presentation.GroovyLanguageServer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Main entry point for the Groovy Language Server.
 */
public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
        // Utility class
    }

    public static void main(String[] args) {
        LOGGER.info("Starting Groovy Language Server...");

        // Create the language server instance
        GroovyLanguageServer server = new GroovyLanguageServer();

        // Create the launcher for the language server
        Launcher<LanguageClient> launcher =
                LSPLauncher.createServerLauncher(server, System.in, System.out);

        // Connect the language client
        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);

        // Start listening for messages
        try {
            launcher.startListening().get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error while listening for messages", e);
            Thread.currentThread().interrupt();
        }
    }
}