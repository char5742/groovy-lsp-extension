import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  LanguageClient,
  type LanguageClientOptions,
  type ServerOptions,
  TransportKind,
} from 'vscode-languageclient/node';
import type { ExtensionApi } from './types';

let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<ExtensionApi> {
  const outputChannel = vscode.window.createOutputChannel('Groovy Language Server');
  outputChannel.appendLine('Groovy Language Server extension is activating...');

  // LSPサーバーJARファイルのパス
  const serverJar = path.join(
    context.extensionPath,
    '..',
    'lsp-core',
    'build',
    'libs',
    'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
  );

  // サーバーオプション
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

  // クライアントオプション
  const clientOptions: LanguageClientOptions = {
    documentSelector: [
      { scheme: 'file', language: 'groovy' },
      { scheme: 'untitled', language: 'groovy' },
    ],
    synchronize: {
      configurationSection: 'groovy-lsp',
      fileEvents: vscode.workspace.createFileSystemWatcher('**/*.{groovy,gradle,gvy,gy,gsh}'),
    },
    outputChannel: outputChannel,
    traceOutputChannel: outputChannel,
  };

  // Language Clientを作成
  client = new LanguageClient('groovy-lsp', 'Groovy Language Server', serverOptions, clientOptions);

  // トレース設定（設定から読み取る）
  const traceServer = vscode.workspace.getConfiguration('groovy-lsp').get<string>('trace.server', 'off');
  await client.setTrace(traceServer as any);

  // デバッグ用にクライアントイベントをログ
  client.onDidChangeState((event) => {
    outputChannel.appendLine(`Language client state changed: ${event.oldState} -> ${event.newState}`);
  });

  // クライアントとサーバーを起動
  try {
    await client.start();
    outputChannel.appendLine('Groovy Language Server started successfully');
  } catch (error) {
    outputChannel.appendLine(`Failed to start Groovy Language Server: ${error}`);
    vscode.window.showErrorMessage(`Failed to start Groovy Language Server: ${error}`);
    throw error;
  }

  return { client };
}

export async function deactivate(): Promise<void> {
  if (client) {
    await client.stop();
  }
}
