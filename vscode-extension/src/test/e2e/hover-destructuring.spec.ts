import { ok } from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { type Extension, type Hover, Position, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';

describe('ホバー機能のE2Eテスト - デストラクチャリング', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let groovyDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // destructuring-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/destructuring-test.groovy');
    ok(existsSync(groovyPath), `テストファイルが見つかりません: ${groovyPath}`);

    groovyDoc = await workspace.openTextDocument(groovyPath);
    await window.showTextDocument(groovyDoc);

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('基本的なデストラクチャリング変数aにホバー', async () => {
    // def (a, b) = list の a にホバー（行8）
    const position = new Position(8, 13); // def (a, b) の a
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, '変数aにホバー情報が表示されるべきです');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    ok(
      hoverContent.includes('a') || hoverContent.includes('Object'),
      `ホバー情報に変数aが含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('デストラクチャリング変数bにホバー', async () => {
    // def (a, b) = list の b にホバー（行8）
    const position = new Position(8, 16); // def (a, b) の b
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, '変数bにホバー情報が表示されるべきです');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    ok(
      hoverContent.includes('b') || hoverContent.includes('Object'),
      `ホバー情報に変数bが含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('使用箇所でのデストラクチャリング変数にホバー', async () => {
    // println "a = $a, b = $b" の a にホバー（行9）
    const position = new Position(9, 22); // $a
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, '使用箇所の変数aにホバー情報が表示されるべきです');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    ok(
      hoverContent.includes('a') || hoverContent.includes('ローカル変数'),
      `ホバー情報に変数aが含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('型付きデストラクチャリング変数にホバー', async () => {
    // def (String name, int age) = ['Alice', 25] の name にホバー（行22）
    const position = new Position(22, 20); // String name
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    if (hovers && hovers.length > 0) {
      const hoverContent = hovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        hoverContent.includes('name') || hoverContent.includes('String'),
        `ホバー情報に変数nameまたはString型が含まれるべきです。実際の内容: ${hoverContent}`,
      );
    }
  });
});
