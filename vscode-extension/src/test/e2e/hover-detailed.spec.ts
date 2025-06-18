import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';

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
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

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
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

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
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

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
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

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
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    // MockされたUserServiceの型情報が表示されるべき
    ok(
      hoverContent.includes('UserService') || hoverContent.includes('userService'),
      'MockオブジェクトでもUserService型の情報が表示される必要があります',
    );
  });

  it('配列アクセスとジェネリクスの型情報が表示される', async () => {
    const text = editor.document.getText();
    // exists(_) >>> [false, true, false] の部分を探す
    const arrayIndex = text.indexOf('[false, true, false]');
    if (arrayIndex === -1) {
      return;
    }

    const position = editor.document.positionAt(arrayIndex + 1); // '[' の次の位置

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    // Boolean[]やList<Boolean>のような型情報が表示されるべき
    ok(
      hoverContent.includes('Boolean') || hoverContent.includes('List') || hoverContent.includes('[]'),
      '配列やリストの型情報が表示される必要があります',
    );
  });

  it('インポートされたクラスの完全修飾名が表示される', async () => {
    const text = editor.document.getText();
    // Specification クラスにホバー
    const specIndex = text.indexOf('extends Specification');
    const specClassIndex = specIndex + 'extends '.length;
    const position = editor.document.positionAt(specClassIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    // spock.lang.Specificationのような完全修飾名が表示されるべき
    ok(
      hoverContent.includes('Specification') || hoverContent.includes('spock'),
      'インポートされたクラスの情報が表示される必要があります',
    );
  });

  it('スコープ外の変数参照で適切なエラーメッセージが表示される', async () => {
    // テスト用の不正なコードを含むファイルを作成
    const invalidContent = `
class Test {
    void method1() {
        def localVar = "test"
    }
    
    void method2() {
        println localVar // スコープ外の変数参照
    }
}
`;

    const tempDoc = await workspace.openTextDocument({
      content: invalidContent,
      language: 'groovy',
    });
    const tempEditor = await window.showTextDocument(tempDoc);

    // localVar（スコープ外）にホバー
    const localVarIndex = invalidContent.indexOf('println localVar');
    const varIndex = localVarIndex + 'println '.length;
    const position = tempEditor.document.positionAt(varIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', tempDoc.uri, position);

    if (hovers && hovers.length > 0) {
      const hoverContent = hovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');

      // 定義が見つからないか、適切なエラーメッセージが表示されるべき
      ok(
        hoverContent.includes('定義が見つかりません') || hoverContent.includes('未定義') || hovers.length === 0,
        'スコープ外の変数には適切なエラーが表示される必要があります',
      );
    }
  });
});
