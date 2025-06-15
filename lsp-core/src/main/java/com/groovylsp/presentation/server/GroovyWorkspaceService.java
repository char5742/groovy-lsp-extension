package com.groovylsp.presentation.server;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

/** Groovyプロジェクト用のワークスペースサービス実装。 */
public class GroovyWorkspaceService implements WorkspaceService {

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    // 将来のマイルストーンで実装予定
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    // 将来のマイルストーンで実装予定
  }
}
