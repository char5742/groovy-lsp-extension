package com.groovylsp.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.presentation.server.GroovyLanguageServer;
import com.groovylsp.testing.IntegrationTest;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;

/** Tests for JSON-RPC communication. */
@IntegrationTest
class JsonRpcCommunicationTest {

  /** Simple test client without telemetry methods to avoid duplicate RPC method issue. */
  static class TestLanguageClient implements LanguageClient {
    @Override
    public void telemetryEvent(Object object) {
      // No-op
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
      // No-op
    }

    @Override
    public void showMessage(MessageParams messageParams) {
      // No-op
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
        ShowMessageRequestParams requestParams) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void logMessage(MessageParams message) {
      // No-op
    }
  }

  @Test
  void testJsonRpcCommunication() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      // Create pipes for communication
      PipedInputStream clientInput = new PipedInputStream();
      PipedOutputStream serverOutput = new PipedOutputStream(clientInput);
      PipedInputStream serverInput = new PipedInputStream();
      PipedOutputStream clientOutput = new PipedOutputStream(serverInput);

      // Create server
      ServerComponent component = DaggerServerComponent.create();
      GroovyLanguageServer server = component.groovyLanguageServer();

      // Create launcher
      Launcher<LanguageClient> serverLauncher =
          LSPLauncher.createServerLauncher(server, serverInput, serverOutput);

      // Connect the actual remote client from the launcher
      server.connect(serverLauncher.getRemoteProxy());

      // Create a simple test client that doesn't have telemetry methods
      TestLanguageClient testClient = new TestLanguageClient();
      Launcher<org.eclipse.lsp4j.services.LanguageServer> clientLauncher =
          LSPLauncher.createClientLauncher(testClient, clientInput, clientOutput);

      // Start both launchers listening
      Future<Void> serverListeningFuture = serverLauncher.startListening();
      Future<Void> clientListeningFuture = clientLauncher.startListening();

      org.eclipse.lsp4j.services.LanguageServer remoteServer = clientLauncher.getRemoteProxy();

      // Send initialize request
      InitializeParams params = new InitializeParams();
      params.setProcessId(12345);
      InitializeResult result = remoteServer.initialize(params).get();

      // Verify response
      assertNotNull(result);
      assertNotNull(result.getCapabilities());

      // Shutdown
      Object shutdownResult = remoteServer.shutdown().get();
      assertEquals(null, shutdownResult);

      // Cancel listening
      serverListeningFuture.cancel(true);
      clientListeningFuture.cancel(true);

      // Clean up
      serverInput.close();
      serverOutput.close();
      clientInput.close();
      clientOutput.close();
    } finally {
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
