import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('ホバー機能のE2Eテスト', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let groovyDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;
  let editor: Awaited<ReturnType<typeof window.showTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // example.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/example.groovy');
    groovyDoc = await workspace.openTextDocument(groovyPath);
    editor = await window.showTextDocument(groovyDoc);

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('クラス名にホバーした際に型情報が表示される', async () => {
    const position = editor.document.positionAt(
      editor.document.getText().indexOf('class User') + 6, // "User" の位置にカーソルを合わせる
    );

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('User') || hoverContent.includes('class') || hoverContent.includes('Groovy'),
      'ホバー内容にUserクラスまたはGroovy要素の情報が含まれる必要があります',
    );
  });

  it('メソッド名にホバーした際にシグネチャが表示される', async () => {
    const position = editor.document.positionAt(
      editor.document.getText().indexOf('User getUser(Long id)') + 5, // "getUser" の位置にカーソルを合わせる
    );

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('getUser') || hoverContent.includes('User') || hoverContent.includes('Groovy'),
      'ホバー内容にgetUserメソッドまたはGroovy要素の情報が含まれる必要があります',
    );
  });

  it('プロパティにホバーした際に型情報が表示される', async () => {
    const position = editor.document.positionAt(editor.document.getText().indexOf('Long id'));

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('id') || hoverContent.includes('Long') || hoverContent.includes('Groovy'),
      'ホバー内容にプロパティの型情報が含まれる必要があります',
    );
  });

  it('フィールドにホバーした際に情報が表示される', async () => {
    const text = editor.document.getText();
    const fieldIndex = text.indexOf('private UserService userService');

    // "UserService" の位置にホバー（型名）
    const typePosition = editor.document.positionAt(fieldIndex + 'private '.length);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, typePosition);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // フィールド名"userService"の位置にもホバー
    const fieldNamePosition = editor.document.positionAt(fieldIndex + 'private UserService '.length);

    const fieldHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      groovyDoc.uri,
      fieldNamePosition,
    );

    if (fieldHovers && fieldHovers.length > 0) {
      const fieldHoverContent = getHoverContent(fieldHovers);
      ok(
        fieldHoverContent.includes('userService') || fieldHoverContent.includes('UserService'),
        'フィールド名からも型情報を取得できる必要があります',
      );
    }

    ok(
      hoverContent.includes('UserService') && !hoverContent.includes('Object'),
      'UserService型の情報が表示され、Objectではない必要があります',
    );
  });

  it('コンストラクタにホバーした際に情報が表示される', async () => {
    const position = editor.document.positionAt(
      editor.document.getText().indexOf('UserController(UserService userService)'),
    );

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('UserController') ||
        hoverContent.includes('constructor') ||
        hoverContent.includes('Groovy'),
      'ホバー内容にコンストラクタ情報が含まれる必要があります',
    );
  });

  it('ローカル変数にホバーした際に型情報が表示される', async () => {
    const text = editor.document.getText();
    const defUserIndex = text.indexOf('def user = new User');
    // "user" 変数名の位置に移動（"def " の4文字分を加算）
    const position = editor.document.positionAt(defUserIndex + 4);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('user') || hoverContent.includes('User') || hoverContent.includes('Groovy'),
      'ホバー内容にローカル変数の型情報が含まれる必要があります',
    );
  });

  it('パラメータにホバーした際に型情報が表示される', async () => {
    const text = editor.document.getText();
    const paramIndex = text.indexOf('createUser(String name') + 'createUser('.length;
    const position = editor.document.positionAt(paramIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('name') || hoverContent.includes('String') || hoverContent.includes('Groovy'),
      'ホバー内容にパラメータの型情報が含まれる必要があります',
    );
  });

  it('型名（Long）にホバーした際に情報が表示される', async () => {
    const text = editor.document.getText();
    const typeIndex = text.indexOf('Long id') + 'Long'.length - 1;
    const position = editor.document.positionAt(typeIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('Long') || hoverContent.includes('type') || hoverContent.includes('Groovy'),
      'ホバー内容に型情報が含まれる必要があります',
    );
  });

  it('メソッド呼び出しにホバーした際に情報が表示される', async () => {
    const position = editor.document.positionAt(editor.document.getText().indexOf('userService.findById'));

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);
    ok(
      hoverContent.includes('findById') || hoverContent.includes('UserService') || hoverContent.includes('Groovy'),
      'ホバー内容にメソッド呼び出し情報が含まれる必要があります',
    );
  });
});
