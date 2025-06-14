package com.groovylsp.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.presentation.server.GroovyLanguageServer;
import com.groovylsp.testing.IntegrationTest;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

/** Integration tests for the Language Server Protocol implementation. */
@IntegrationTest
class LanguageServerIntegrationTest {

  private GroovyLanguageServer server;
  private LanguageClient mockClient;

  @BeforeEach
  void setUp() {
    ServerComponent component = DaggerServerComponent.create();
    server = component.groovyLanguageServer();
    mockClient = Mockito.mock(LanguageClient.class);
    server.connect(mockClient);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void testFullInitializationSequence() throws Exception {
    // Given
    InitializeParams params = new InitializeParams();
    params.setProcessId(12345);
    WorkspaceFolder workspaceFolder = new WorkspaceFolder();
    workspaceFolder.setUri("file:///test/project");
    workspaceFolder.setName("test-project");
    params.setWorkspaceFolders(Arrays.asList(workspaceFolder));

    // When - Initialize
    CompletableFuture<InitializeResult> initFuture = server.initialize(params);
    InitializeResult initResult = initFuture.get();

    // Then
    assertNotNull(initResult);
    assertNotNull(initResult.getCapabilities());

    // When - Shutdown
    CompletableFuture<Object> shutdownFuture = server.shutdown();
    shutdownFuture.get();

    // Then
    assertTrue(shutdownFuture.isDone());
  }

  @Test
  @IntegrationTest
  void testServerCapabilities() throws Exception {
    // Given
    InitializeParams params = new InitializeParams();

    // When
    InitializeResult result = server.initialize(params).get();

    // Then
    assertNotNull(result.getCapabilities());
    // For now, we don't advertise any capabilities
    // These assertions will be added as capabilities are implemented
  }

  @Test
  @IntegrationTest
  void testMultipleInitializationRequests() throws Exception {
    // Given
    InitializeParams params1 = new InitializeParams();
    params1.setProcessId(1);
    InitializeParams params2 = new InitializeParams();
    params2.setProcessId(2);

    // When
    InitializeResult result1 = server.initialize(params1).get();
    InitializeResult result2 = server.initialize(params2).get();

    // Then
    assertNotNull(result1);
    assertNotNull(result2);
    // Both should return valid results even if called multiple times
    assertEquals(result1.getCapabilities(), result2.getCapabilities());
  }
}
