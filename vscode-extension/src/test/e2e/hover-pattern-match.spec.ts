import { ok } from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { type Extension, type Hover, Position, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('ホバー機能のE2Eテスト - パターンマッチング', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let groovyDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // pattern-match-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/pattern-match-test.groovy');
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

  it('型パターンマッチングの型名にホバー', async () => {
    // case String s: の "String" にホバー
    const position = new Position(29, 18); // case String s: の String
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'Stringの型にホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);

    ok(
      hoverContent.includes('String') || hoverContent.includes('class'),
      `ホバー情報にStringまたはclassが含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('パターンマッチング変数にホバー', async () => {
    // case String s: のブロック内の変数 s にホバー
    const position = new Position(30, 40); // println "String: $s" の s
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, '変数sにホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);

    ok(
      hoverContent.includes('s') || hoverContent.includes('String') || hoverContent.includes('element'),
      `ホバー情報に変数sまたはString型が含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });
});
