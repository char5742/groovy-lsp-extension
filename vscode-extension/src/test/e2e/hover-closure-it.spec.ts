import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';

describe('クロージャ変数itのホバーE2Eテスト', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let closureItDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;
  let editor: Awaited<ReturnType<typeof window.showTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // closure-it-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/closure-it-test.groovy');
    closureItDoc = await workspace.openTextDocument(groovyPath);
    editor = await window.showTextDocument(closureItDoc);

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('Integerリストのeachクロージャ内でitがInteger型として表示される', async () => {
    const text = editor.document.getText();
    // "println it  // itはInteger型" の it にホバー
    const itIndex = text.indexOf('println it  // itはInteger型') + 'println '.length;
    const position = editor.document.positionAt(itIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    ok(hoverContent.includes('Integer') || hoverContent.includes('int'), 'Integer型の情報が含まれる必要があります');
  });

  it('Stringリストのeachクロージャ内でitがString型として表示される', async () => {
    const text = editor.document.getText();
    // "it.toUpperCase()  // itはString型" の最初の it にホバー
    const itIndex = text.indexOf('it.toUpperCase()  // itはString型');
    const position = editor.document.positionAt(itIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    ok(hoverContent.includes('String'), 'String型の情報が含まれる必要があります');
  });

  it('MapのeachクロージャでitがMap.Entry型として表示される', async () => {
    const text = editor.document.getText();
    // "${it.key}: ${it.value}" の最初の it にホバー
    const itKeyIndex = text.indexOf('${it.key}') + '${'.length;
    const position = editor.document.positionAt(itKeyIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    // Map.Entry型の情報が含まれることを確認
    ok(hoverContent.includes('Entry') || hoverContent.includes('Map'), 'Map.Entry型の情報が含まれる必要があります');
  });

  it('findAllクロージャ内でitが正しい型として表示される', async () => {
    const text = editor.document.getText();
    // "it % 2 == 0" の it にホバー
    const itIndex = text.indexOf('it % 2 == 0');
    const position = editor.document.positionAt(itIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    ok(hoverContent.includes('Integer') || hoverContent.includes('int'), 'Integer型の情報が含まれる必要があります');
  });

  it('カスタムクラスのリストでitが正しい型として表示される', async () => {
    const text = editor.document.getText();
    // "println it.name  // itはPerson型" の it にホバー
    const itIndex = text.indexOf('println it.name  // itはPerson型') + 'println '.length;
    const position = editor.document.positionAt(itIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    ok(hoverContent.includes('Person'), 'Person型の情報が含まれる必要があります');
  });

  it('ネストされたクロージャで内側のitが正しい型として表示される', async () => {
    const text = editor.document.getText();
    // 内側の "println it  // itはInteger型（内側のクロージャ）" の it にホバー
    const innerItText = 'println it  // itはInteger型（内側のクロージャ）';
    const itIndex = text.indexOf(innerItText) + 'println '.length;
    const position = editor.document.positionAt(itIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    ok(hoverContent.includes('Integer') || hoverContent.includes('int'), 'Integer型の情報が含まれる必要があります');
  });

  it('timesメソッドのクロージャでitがInteger型として表示される', async () => {
    const text = editor.document.getText();
    // "Count: $it" の it にホバー
    const itIndex = text.indexOf('Count: $it') + 'Count: $'.length;
    const position = editor.document.positionAt(itIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', closureItDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('it'), 'it変数の情報が表示される必要があります');
    ok(
      hoverContent.includes('Integer') || hoverContent.includes('int'),
      'timesメソッドのitはInteger型である必要があります',
    );
  });
});
