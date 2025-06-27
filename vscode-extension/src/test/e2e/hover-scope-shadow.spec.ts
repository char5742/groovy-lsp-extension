import { ok } from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { type Extension, type Hover, Position, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('ホバー機能のE2Eテスト - スコープシャドウイング', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let groovyDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // scope-shadow-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/scope-shadow-test.groovy');
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

  it('クラスレベルの変数nameにホバー', async () => {
    // static String name = "Class level" の name（行4）
    const position = new Position(4, 18); // static String name
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'クラスフィールドnameにホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('name') && hoverContent.includes('String'),
      `ホバー情報にnameとString型が含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('メソッドレベルでシャドウされた変数nameにホバー', async () => {
    // String name = "Method level" の name（行12）
    const position = new Position(12, 15); // String name
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'メソッドローカル変数nameにホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('name') && (hoverContent.includes('String') || hoverContent.includes('ローカル変数')),
      `ホバー情報にローカル変数nameが含まれるべきです。実際の内容: ${hoverContent}`,
    );
  });

  it('ブロック内でさらにシャドウされた変数nameにホバー', async () => {
    // String name = "Block level" の使用箇所（行20）
    // println "Block scope - name: $name" の $name
    const position = new Position(19, 41); // $name
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    if (hovers && hovers.length > 0) {
      const hoverContent = getHoverContent(hovers);
      ok(hoverContent.includes('name'), `ホバー情報に変数nameが含まれるべきです。実際の内容: ${hoverContent}`);
    }
  });

  it('クロージャ内でシャドウされた変数にホバー', async () => {
    // String outer = "Closure scope" の outer（行32）
    const position = new Position(32, 19); // String outer
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'クロージャ内変数outerにホバー情報が表示されるべきです');
    const hoverContent = getHoverContent(hovers);
    ok(hoverContent.includes('outer'), `ホバー情報に変数outerが含まれるべきです。実際の内容: ${hoverContent}`);
  });

  it('thisを使ったインスタンスフィールドアクセスにホバー', async () => {
    // this.instanceName の instanceName（行54）
    const position = new Position(53, 62); // this.instanceName
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    if (hovers && hovers.length > 0) {
      const hoverContent = getHoverContent(hovers);
      ok(
        hoverContent.includes('instanceName') || hoverContent.includes('String'),
        `ホバー情報にinstanceNameフィールドが含まれるべきです。実際の内容: ${hoverContent}`,
      );
    }
  });
});
