package com.groovylsp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.groovylsp.infrastructure.di.DaggerServerComponent;
import com.groovylsp.infrastructure.di.ServerComponent;
import com.groovylsp.presentation.server.GroovyLanguageServer;
import com.groovylsp.testing.IntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/** 行カウント機能の統合テスト */
@IntegrationTest
class LineCountIntegrationTest {

  private GroovyLanguageServer server;
  private LanguageClient mockClient;

  @BeforeEach
  void setUp() {
    ServerComponent component = DaggerServerComponent.create();
    server = component.groovyLanguageServer();
    mockClient = mock(LanguageClient.class);
    server.connect(mockClient);
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void ドキュメントを開いた時に行カウント診断が表示される() throws Exception {
    // サーバーを初期化
    var initParams = new InitializeParams();
    initParams.setProcessId(12345);
    var workspaceFolder = new WorkspaceFolder();
    workspaceFolder.setUri("file:///test/project");
    workspaceFolder.setName("test-project");
    initParams.setWorkspaceFolders(Arrays.asList(workspaceFolder));

    CompletableFuture<InitializeResult> initFuture = server.initialize(initParams);
    InitializeResult initResult = initFuture.get(2, TimeUnit.SECONDS);
    assertThat(initResult).isNotNull();

    server.initialized(new InitializedParams());

    // ドキュメントを開く
    var textDocumentItem = new TextDocumentItem();
    textDocumentItem.setUri("file:///test/project/Hello.groovy");
    textDocumentItem.setLanguageId("groovy");
    textDocumentItem.setVersion(1);
    textDocumentItem.setText(
        """
        // Groovyのサンプルコード
        class HelloWorld {

            static void main(String[] args) {
                println 'Hello, World!'
            }
        }
        """);

    var openParams = new DidOpenTextDocumentParams();
    openParams.setTextDocument(textDocumentItem);

    // When
    server.getTextDocumentService().didOpen(openParams);

    // Then - 診断が送信されることを確認
    Thread.sleep(100); // 非同期処理の完了を待つ

    ArgumentCaptor<PublishDiagnosticsParams> captor =
        ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient, atLeastOnce()).publishDiagnostics(captor.capture());

    PublishDiagnosticsParams diagnosticsParams = captor.getValue();
    assertThat(diagnosticsParams.getUri()).isEqualTo("file:///test/project/Hello.groovy");

    List<Diagnostic> diagnostics = diagnosticsParams.getDiagnostics();
    assertThat(diagnostics).hasSize(1);

    Diagnostic diagnostic = diagnostics.get(0);
    assertThat(diagnostic.getSeverity()).isEqualTo(DiagnosticSeverity.Information);
    assertThat(diagnostic.getMessage()).contains("総行数: 8行");
    assertThat(diagnostic.getMessage()).contains("コード: 5行");
    assertThat(diagnostic.getMessage()).contains("空行: 2行");
    assertThat(diagnostic.getMessage()).contains("コメント: 1行");
    assertThat(diagnostic.getSource()).isEqualTo("groovy-lsp-line-count");
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void 空のドキュメントでも行カウント診断が表示される() throws Exception {
    // サーバーを初期化
    var initParams = new InitializeParams();
    initParams.setProcessId(12345);
    server.initialize(initParams).get(2, TimeUnit.SECONDS);
    server.initialized(new InitializedParams());

    // 空のドキュメントを開く
    var textDocumentItem = new TextDocumentItem();
    textDocumentItem.setUri("file:///test/Empty.groovy");
    textDocumentItem.setLanguageId("groovy");
    textDocumentItem.setVersion(1);
    textDocumentItem.setText("");

    var openParams = new DidOpenTextDocumentParams();
    openParams.setTextDocument(textDocumentItem);

    // When
    server.getTextDocumentService().didOpen(openParams);

    // Then
    Thread.sleep(100);

    ArgumentCaptor<PublishDiagnosticsParams> captor =
        ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient, atLeastOnce()).publishDiagnostics(captor.capture());

    PublishDiagnosticsParams diagnosticsParams = captor.getValue();
    List<Diagnostic> diagnostics = diagnosticsParams.getDiagnostics();
    assertThat(diagnostics).hasSize(1);

    Diagnostic diagnostic = diagnostics.get(0);
    assertThat(diagnostic.getMessage()).isEqualTo("総行数: 0行 (コード: 0行, 空行: 0行, コメント: 0行)");
  }

  @Test
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  void ドキュメント変更時に行カウントが更新される() throws Exception {
    // サーバーを初期化
    var initParams = new InitializeParams();
    initParams.setProcessId(12345);
    server.initialize(initParams).get(2, TimeUnit.SECONDS);
    server.initialized(new InitializedParams());

    // ドキュメントを開く
    var textDocumentItem = new TextDocumentItem();
    textDocumentItem.setUri("file:///test/Dynamic.groovy");
    textDocumentItem.setLanguageId("groovy");
    textDocumentItem.setVersion(1);
    textDocumentItem.setText("println 'Hello'");

    var openParams = new DidOpenTextDocumentParams();
    openParams.setTextDocument(textDocumentItem);
    server.getTextDocumentService().didOpen(openParams);

    Thread.sleep(100);
    reset(mockClient);

    // ドキュメントを変更
    var changeParams = new DidChangeTextDocumentParams();
    var versionedId = new VersionedTextDocumentIdentifier();
    versionedId.setUri("file:///test/Dynamic.groovy");
    versionedId.setVersion(2);
    changeParams.setTextDocument(versionedId);

    var contentChange = new TextDocumentContentChangeEvent();
    contentChange.setText(
        """
        println 'Hello'
        // 新しいコメント

        println 'World'
        """);
    changeParams.setContentChanges(Arrays.asList(contentChange));

    // When
    server.getTextDocumentService().didChange(changeParams);

    // Then
    Thread.sleep(100);

    ArgumentCaptor<PublishDiagnosticsParams> captor =
        ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
    verify(mockClient, atLeastOnce()).publishDiagnostics(captor.capture());

    PublishDiagnosticsParams diagnosticsParams = captor.getValue();
    List<Diagnostic> diagnostics = diagnosticsParams.getDiagnostics();
    assertThat(diagnostics).hasSize(1);

    Diagnostic diagnostic = diagnostics.get(0);
    assertThat(diagnostic.getMessage()).contains("総行数: 5行");
    assertThat(diagnostic.getMessage()).contains("コード: 2行");
    assertThat(diagnostic.getMessage()).contains("空行: 2行");
    assertThat(diagnostic.getMessage()).contains("コメント: 1行");
  }
}
