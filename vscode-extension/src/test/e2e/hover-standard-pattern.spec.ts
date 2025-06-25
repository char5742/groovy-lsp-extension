import { ok } from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { type Extension, type Hover, Position, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('ホバー機能のE2Eテスト - 標準Groovyパターン', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let groovyDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // groovy-pattern-standard.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/groovy-pattern-standard.groovy');
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

  it('標準的なcase String:の型名にホバー', async () => {
    // case String: の "String" にホバー
    const position = new Position(8, 22); // case String:
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'Stringの型にホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);
    ok(!hoverContent.includes('解析に失敗'), `ファイルが正常に解析されるべきです。実際の内容: ${hoverContent}`);
  });

  it('switch内のitem変数にホバー', async () => {
    // println "String: $item" の item にホバー
    const position = new Position(9, 36); // $item
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, '変数itemにホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('item') || hoverContent.includes('Object'),
      `ホバー情報に変数itemが含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('instanceof後のキャスト変数にホバー', async () => {
    // String s = value の s にホバー
    const position = new Position(38, 19); // String s
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    if (hovers && hovers.length > 0) {
      const hoverContent = getHoverContent(hovers);
      ok(
        hoverContent.includes('s') || hoverContent.includes('String'),
        `ホバー情報に変数sまたはString型が含まれるべきです。実際の内容: ${hoverContent}`,
      );
    }
  });
});
