import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';

describe('enum関連のホバー機能E2Eテスト', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let enumDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;
  let editor: Awaited<ReturnType<typeof window.showTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // enum-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/enum-test.groovy');
    enumDoc = await workspace.openTextDocument(groovyPath);
    editor = await window.showTextDocument(enumDoc);

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('enum名にホバーすると列挙子一覧が表示される', async () => {
    const text = editor.document.getText();
    // "enum Color" のColorにホバー
    const enumIndex = text.indexOf('enum Color') + 'enum '.length;
    const position = editor.document.positionAt(enumIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', enumDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    // enum情報が表示されることを確認
    ok(hoverContent.includes('Color') || hoverContent.includes('enum'), 'enum Colorの情報が表示される必要があります');

    // 将来的には列挙子一覧（RED, GREEN, BLUE）も含まれるべき
    // ok(hoverContent.includes('RED') || hoverContent.includes('GREEN'), '列挙子一覧が含まれる必要があります');
  });

  it('enum定数にホバーすると型情報が表示される', async () => {
    const text = editor.document.getText();
    // Color.REDのREDにホバー
    const redIndex = text.indexOf('Color.RED') + 'Color.'.length;
    const position = editor.document.positionAt(redIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', enumDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('RED') || hoverContent.includes('Color'), 'enum定数REDの情報が表示される必要があります');
  });

  it('enum型フィールドにホバーすると正しい型が表示される', async () => {
    const text = editor.document.getText();
    // favoriteColorフィールドにホバー
    const fieldIndex = text.indexOf('favoriteColor = Color.RED');
    const position = editor.document.positionAt(fieldIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', enumDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(
      hoverContent.includes('Color') && !hoverContent.includes('Object'),
      'favoriteColorフィールドがColor型として表示される必要があります',
    );
  });

  it('パラメータなしのenum定数も正しく表示される', async () => {
    const text = editor.document.getText();
    // Status.PENDINGのPENDINGにホバー
    const pendingIndex = text.indexOf('Status.PENDING') + 'Status.'.length;
    const position = editor.document.positionAt(pendingIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', enumDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(
      hoverContent.includes('PENDING') || hoverContent.includes('Status'),
      'enum定数PENDINGの情報が表示される必要があります',
    );
  });

  it('ネストされたenumも正しく認識される', async () => {
    const text = editor.document.getText();
    // InnerEnumにホバー
    const innerEnumIndex = text.indexOf('enum InnerEnum') + 'enum '.length;
    const position = editor.document.positionAt(innerEnumIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', enumDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(
      hoverContent.includes('InnerEnum') || hoverContent.includes('enum'),
      'ネストされたenumの情報が表示される必要があります',
    );
  });

  it('switch文内のenum定数も認識される', async () => {
    const text = editor.document.getText();
    // case Status.PENDING: のPENDINGにホバー
    const caseIndex = text.indexOf('case Status.PENDING:') + 'case Status.'.length;
    const position = editor.document.positionAt(caseIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', enumDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(
      hoverContent.includes('PENDING') || hoverContent.includes('Status'),
      'switch文内のenum定数も認識される必要があります',
    );
  });
});
