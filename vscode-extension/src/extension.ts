import { join } from 'node:path';
import { type ExtensionContext, commands, window, workspace } from 'vscode';
import {
  LanguageClient,
  type LanguageClientOptions,
  type ServerOptions,
  Trace,
  TransportKind,
} from 'vscode-languageclient/node';
import type { ExtensionApi } from './types.ts';

let client: LanguageClient | undefined;

export async function activate(context: ExtensionContext): Promise<ExtensionApi> {
  const outputChannel = window.createOutputChannel('Groovy Language Server');
  outputChannel.appendLine('Groovy Language Server extension is activating...');

  // LSPサーバーJARファイルのパス
  const serverJar = join(
    context.extensionPath,
    'server',
    'groovy-lsp-server.jar',
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
      args: ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005,quiet=y', '-jar', serverJar],
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
      fileEvents: workspace.createFileSystemWatcher('**/*.{groovy,gradle,gvy,gy,gsh}'),
    },
    outputChannel: outputChannel,
    traceOutputChannel: outputChannel,
  };

  // Language Clientを作成
  client = new LanguageClient('groovy-lsp', 'Groovy Language Server', serverOptions, clientOptions);

  // トレース設定（設定から読み取る）
  const traceServer = workspace.getConfiguration('groovy-lsp').get<string>('trace.server', 'off');
  const traceValue =
    traceServer === 'verbose' ? Trace.Verbose : traceServer === 'messages' ? Trace.Messages : Trace.Off;
  await client.setTrace(traceValue);

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
    window.showErrorMessage(`Failed to start Groovy Language Server: ${error}`);
    throw error;
  }

  // コマンドを登録
  const restartCommand = commands.registerCommand('groovy-lsp.restartServer', async () => {
    outputChannel.appendLine('Restarting Groovy Language Server...');
    if (client) {
      await client.restart();
      outputChannel.appendLine('Groovy Language Server restarted successfully');
      window.showInformationMessage('Groovy Language Server restarted');
    }
  });

  const showOutputCommand = commands.registerCommand('groovy-lsp.showOutputChannel', () => {
    outputChannel.show();
  });

  context.subscriptions.push(restartCommand, showOutputCommand);

  return { client };
}

export async function deactivate(): Promise<void> {
  if (client) {
    await client.stop();
  }
}
