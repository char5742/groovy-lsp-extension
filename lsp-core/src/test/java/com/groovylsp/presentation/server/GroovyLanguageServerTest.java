package com.groovylsp.presentation.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.testing.FastTest;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for GroovyLanguageServer. */
class GroovyLanguageServerTest {

  private GroovyLanguageServer server;

  @BeforeEach
  void setUp() {
    ServerComponent component = DaggerServerComponent.create();
    server = component.groovyLanguageServer();
  }

  @Test
  @FastTest
  void testInitialize() throws Exception {
    // Given
    var params = new InitializeParams();

    // When
    CompletableFuture<InitializeResult> future = server.initialize(params);
    InitializeResult result = future.get();

    // Then
    assertNotNull(result);
    assertNotNull(result.getCapabilities());
  }

  @Test
  @FastTest
  void testInitialized() {
    // Given
    var params = new InitializedParams();

    // When/Then - Should not throw exception
    server.initialized(params);
  }

  @Test
  @FastTest
  void testShutdown() throws Exception {
    // When
    CompletableFuture<Object> future = server.shutdown();
    future.get();

    // Then
    assertTrue(future.isDone());
  }

  @Test
  @FastTest
  void testGetTextDocumentService() {
    // When
    var service = server.getTextDocumentService();

    // Then
    assertNotNull(service);
    assertTrue(service instanceof GroovyTextDocumentService);
  }

  @Test
  @FastTest
  void testGetWorkspaceService() {
    // When
    var service = server.getWorkspaceService();

    // Then
    assertNotNull(service);
    assertTrue(service instanceof GroovyWorkspaceService);
  }
}
