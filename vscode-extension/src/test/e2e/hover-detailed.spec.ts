import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('ホバー機能の詳細なE2Eテスト', () => {
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

  it('メソッド呼び出しの詳細な型情報が表示される', async () => {
    const text = editor.document.getText();
    // userService.findById(id) の findById にホバー
    const findByIdIndex = text.indexOf('userService.findById(id)');
    const position = editor.document.positionAt(findByIdIndex + 'userService.'.length);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // 期待される内容：User findById(Long id) のような具体的な型情報
    ok(!hoverContent.includes('定義が見つかりません'), 'メソッド呼び出しの定義が見つかる必要があります');
    ok(hoverContent.includes('findById'), 'メソッド名が含まれる必要があります');
    ok(!hoverContent.includes('Object'), 'Objectではなく具体的な型が表示される必要があります');
  });

  it('変数参照時に正確な型情報が表示される', async () => {
    const text = editor.document.getText();
    // result変数（controller.getUser(1L)の戻り値）にホバー
    const resultIndex = text.indexOf('def result = controller.getUser(1L)');
    const resultVarIndex = resultIndex + 'def '.length;
    const position = editor.document.positionAt(resultVarIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // resultの型はUserであるべき
    ok(
      hoverContent.includes('User') || hoverContent.includes('result'),
      'result変数の型情報（User）が表示される必要があります',
    );
  });

  it('メソッドチェーンでの型情報が正確に表示される', async () => {
    const text = editor.document.getText();
    // result.name のnameプロパティにホバー
    const resultNameIndex = text.indexOf('result.name == "John"');
    const nameIndex = resultNameIndex + 'result.'.length;
    const position = editor.document.positionAt(nameIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // nameプロパティの型はStringであるべき
    ok(
      hoverContent.includes('String') || hoverContent.includes('name'),
      'nameプロパティの型情報（String）が表示される必要があります',
    );
  });

  it('Groovyの動的メソッド呼び出しでも型情報が表示される', async () => {
    const text = editor.document.getText();
    // new User(name: name, email: email) のUserコンストラクタにホバー
    const newUserIndex = text.indexOf('new User(name: name');
    const userCtorIndex = newUserIndex + 'new '.length;
    const position = editor.document.positionAt(userCtorIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // コンストラクタ情報が表示されるべき
    ok(hoverContent.includes('User'), 'User型の情報が表示される必要があります');
  });

  it('Mock/Stubオブジェクトの型情報が表示される', async () => {
    const text = editor.document.getText();
    // userService（Mock）の使用箇所にホバー
    const mockUsageIndex = text.indexOf('1 * userService.findById(1L)');
    const userServiceIndex = mockUsageIndex + '1 * '.length;
    const position = editor.document.positionAt(userServiceIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // MockされたUserServiceの型情報が表示されるべき
    ok(
      hoverContent.includes('UserService') || hoverContent.includes('userService'),
      'MockオブジェクトでもUserService型の情報が表示される必要があります',
    );
  });
});
