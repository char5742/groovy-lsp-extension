import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  LanguageClient,
  type LanguageClientOptions,
  type ServerOptions,
  TransportKind,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const outputChannel = vscode.window.createOutputChannel('Groovy Language Server');
  outputChannel.appendLine('Groovy Language Server extension is activating...');

  // Path to the LSP server JAR file
  const serverJar = path.join(
    context.extensionPath,
    '..',
    'lsp-core',
    'build',
    'libs',
    'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
  );

  // Server options
  const serverOptions: ServerOptions = {
    run: {
      command: 'java',
      args: ['-jar', serverJar],
      transport: TransportKind.stdio,
    },
    debug: {
      command: 'java',
      args: ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-jar', serverJar],
      transport: TransportKind.stdio,
    },
  };

  // Client options
  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: 'file', language: 'groovy' }],
    synchronize: {
      configurationSection: 'groovy-lsp',
      fileEvents: vscode.workspace.createFileSystemWatcher('**/*.{groovy,gradle,gvy,gy,gsh}'),
    },
    outputChannel: outputChannel,
    traceOutputChannel: outputChannel,
  };

  // Create the language client
  client = new LanguageClient('groovy-lsp', 'Groovy Language Server', serverOptions, clientOptions);

  // Log client events for debugging
  client.onDidChangeState((event) => {
    outputChannel.appendLine(`Language client state changed: ${event.oldState} -> ${event.newState}`);
  });

  // Start the client and the server
  try {
    await client.start();
    outputChannel.appendLine('Groovy Language Server started successfully');
  } catch (error) {
    outputChannel.appendLine(`Failed to start Groovy Language Server: ${error}`);
    vscode.window.showErrorMessage(`Failed to start Groovy Language Server: ${error}`);
  }
}

export async function deactivate(): Promise<void> {
  if (client) {
    await client.stop();
  }
}
