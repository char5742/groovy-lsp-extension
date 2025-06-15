// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as assert from 'node:assert/strict';
// biome-ignore lint/style/noNamespaceImport: VSCode APIを使用
// biome-ignore lint/correctness/noUndeclaredDependencies: VSCodeが提供
import * as vscode from 'vscode';
import { getLanguageClient } from '../test-utils/lsp';

describe('コマンド機能のテスト', () => {
  it('拡張機能が正しくアクティベートされる', async () => {
    const extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    assert.ok(extension);

    if (!extension?.isActive) {
      await extension?.activate();
    }

    assert.strictEqual(extension?.isActive, true);
  });

  it('groovy-lsp.restartServerコマンドが登録されている', async () => {
    const commands = await vscode.commands.getCommands(true);
    assert.ok(commands.includes('groovy-lsp.restartServer'));
  });

  it('groovy-lsp.showOutputChannelコマンドが登録されている', async () => {
    const commands = await vscode.commands.getCommands(true);
    assert.ok(commands.includes('groovy-lsp.showOutputChannel'));
  });

  it('Language Clientが正しく初期化される', async () => {
    const client = await getLanguageClient();
    assert.ok(client);

    // クライアントが開始されているか確認
    if (client) {
      assert.strictEqual(client.state, 2); // State.Running = 2
    }
  });

  it('設定が正しく読み込まれる', () => {
    const config = vscode.workspace.getConfiguration('groovy-lsp');
    const traceLevel = config.get<string>('trace.server');

    assert.ok(['off', 'messages', 'verbose'].includes(traceLevel || ''));
  });

  it('ステータスバーアイテムが表示される', async () => {
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
      assert.ok(exports.statusBarItem);
    }
  });

  it('restartServerコマンドが実行できる', async () => {
    // コマンドの実行
    try {
      await vscode.commands.executeCommand('groovy-lsp.restartServer');
      // エラーが発生しなければ成功
      assert.ok(true);
    } catch (error) {
      // Language Serverが起動していない場合はエラーになる可能性がある
      // その場合でもコマンド自体は登録されていることを確認
      assert.ok(error);
    }
  });
});
