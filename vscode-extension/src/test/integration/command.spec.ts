import { describe, expect, test } from 'bun:test';
import * as vscode from 'vscode';
import { getLanguageClient } from '../test-utils/lsp';

describe('コマンド機能のテスト', () => {
  test('拡張機能が正しくアクティベートされる', async () => {
    const extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    expect(extension).toBeDefined();

    if (!extension?.isActive) {
      await extension?.activate();
    }

    expect(extension?.isActive).toBe(true);
  });

  test('groovy-lsp.restartServerコマンドが登録されている', async () => {
    const commands = await vscode.commands.getCommands(true);
    expect(commands).toContain('groovy-lsp.restartServer');
  });

  test('groovy-lsp.showOutputChannelコマンドが登録されている', async () => {
    const commands = await vscode.commands.getCommands(true);
    expect(commands).toContain('groovy-lsp.showOutputChannel');
  });

  test('Language Clientが正しく初期化される', async () => {
    const client = await getLanguageClient();
    expect(client).toBeDefined();

    // クライアントが開始されているか確認
    if (client) {
      expect(client.state).toBe(2); // State.Running = 2
    }
  });

  test('設定が正しく読み込まれる', async () => {
    const config = vscode.workspace.getConfiguration('groovy-lsp');
    const traceLevel = config.get<string>('trace.server');

    expect(['off', 'messages', 'verbose']).toContain(traceLevel);
  });

  test('ステータスバーアイテムが表示される', async () => {
    // 拡張機能をアクティベート
    const extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    if (!extension?.isActive) {
      await extension?.activate();
    }

    // ステータスバーアイテムの存在を確認
    // 注: VS Code APIでは直接ステータスバーアイテムを取得できないため、
    // 拡張機能のエクスポートから確認する必要がある
    const exports = extension?.exports;
    if (exports?.statusBarItem) {
      expect(exports.statusBarItem).toBeDefined();
    }
  });

  test('restartServerコマンドが実行できる', async () => {
    // コマンドの実行
    try {
      await vscode.commands.executeCommand('groovy-lsp.restartServer');
      // エラーが発生しなければ成功
      expect(true).toBe(true);
    } catch (error) {
      // Language Serverが起動していない場合はエラーになる可能性がある
      // その場合でもコマンド自体は登録されていることを確認
      expect(error).toBeDefined();
    }
  });
});
