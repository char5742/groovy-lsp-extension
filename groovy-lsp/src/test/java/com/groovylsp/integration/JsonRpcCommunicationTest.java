package com.groovylsp.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.presentation.server.GroovyLanguageServer;
import com.groovylsp.testing.IntegrationTest;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Future;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Tests for JSON-RPC communication. */
@IntegrationTest
class JsonRpcCommunicationTest {

  @Test
  @org.junit.jupiter.api.Disabled("Temporarily disabled due to mock client issue")
  void testJsonRpcCommunication() throws Exception {
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

    // Mock client
    LanguageClient mockClient = Mockito.mock(LanguageClient.class);
    server.connect(mockClient);

    // Start listening in a separate thread
    Future<Void> listening = serverLauncher.startListening();

    // Create client-side launcher to send requests
    Launcher<org.eclipse.lsp4j.services.LanguageServer> clientLauncher =
        LSPLauncher.createClientLauncher(mockClient, clientInput, clientOutput);
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

    // Clean up
    serverInput.close();
    serverOutput.close();
    clientInput.close();
    clientOutput.close();
  }
}
