package com.groovylsp.presentation.server;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

/** Workspace service implementation for Groovy projects. */
public class GroovyWorkspaceService implements WorkspaceService {

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    // Will be implemented in future milestones
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    // Will be implemented in future milestones
  }
}
