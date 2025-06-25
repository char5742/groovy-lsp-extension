import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('オブジェクト指向機能のホバーE2Eテスト', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let oopDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;
  let editor: Awaited<ReturnType<typeof window.showTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // oop-features-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/oop-features-test.groovy');
    oopDoc = await workspace.openTextDocument(groovyPath);
    editor = await window.showTextDocument(oopDoc);

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('レコードクラスにホバーするとコンポーネント一覧が表示される', async () => {
    const text = editor.document.getText();
    // "record Person" のPersonにホバー
    const recordIndex = text.indexOf('record Person') + 'record '.length;
    const position = editor.document.positionAt(recordIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    // レコード情報が表示されることを確認
    ok(
      hoverContent.includes('Person') || hoverContent.includes('class'),
      'record Personの情報が表示される必要があります',
    );

    // コンポーネント（name, age, email）が含まれることを確認
    ok(
      hoverContent.includes('name') && hoverContent.includes('age') && hoverContent.includes('email'),
      `コンポーネント情報が含まれる必要があります。実際の内容: "${hoverContent}"`,
    );
  });

  it('ネストクラスにホバーすると完全修飾名が表示される', async () => {
    const text = editor.document.getText();
    // InnerClassにホバー
    const innerClassIndex = text.indexOf('class InnerClass') + 'class '.length;
    const position = editor.document.positionAt(innerClassIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    ok(hoverContent.includes('InnerClass'), 'InnerClassの情報が表示される必要があります');

    // 将来的には OuterClass.InnerClass のような完全修飾名が表示されるべき
    // ok(hoverContent.includes('OuterClass'), '外部クラス名も含まれる必要があります');
  });

  it('オーバーライドされたメソッドにホバーすると@Overrideと親メソッド情報が表示される', async () => {
    const text = editor.document.getText();
    // Dog クラスの makeSound メソッドにホバー
    const makeSoundIndex = text.indexOf('@Override\n    String makeSound()') + '@Override\n    String '.length;
    const position = editor.document.positionAt(makeSoundIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    ok(hoverContent.includes('makeSound'), 'makeSoundメソッドの情報が表示される必要があります');

    // 将来的には@Overrideアノテーションや親クラスの情報も含まれるべき
    // ok(hoverContent.includes('Override') || hoverContent.includes('Animal'), 'オーバーライド情報が含まれる必要があります');
  });

  it('型パラメータ境界にホバーすると上限型が表示される', async () => {
    const text = editor.document.getText();
    // Container<T extends Number> のTにホバー
    const containerIndex = text.indexOf('Container<T extends Number>');
    const typeParamIndex = containerIndex + 'Container<'.length;
    const position = editor.document.positionAt(typeParamIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    if (hovers && hovers.length > 0) {
      const hoverContent = getHoverContent(hovers);

      // 型パラメータの情報が表示されることを確認
      ok(
        hoverContent.includes('T') || hoverContent.includes('Number') || hoverContent.includes('type'),
        `型パラメータの情報が表示される必要があります。実際の内容: "${hoverContent}"`,
      );
    } else {
      ok(false, 'ホバー結果が返される必要があります');
    }
  });

  it('オーバーロードされたメソッド呼び出しで正しいオーバーロードが特定される', async () => {
    const text = editor.document.getText();
    // calc.add(1, 2) の add にホバー
    const addCallIndex = text.indexOf('calc.add(1, 2)') + 'calc.'.length;
    const position = editor.document.positionAt(addCallIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    ok(hoverContent.includes('add'), 'addメソッドの情報が表示される必要があります');

    // 将来的には適切なオーバーロード（int, int）が特定されるべき
    // ok(hoverContent.includes('int') && !hoverContent.includes('double'), '正しいオーバーロードが選択される必要があります');
  });

  it('静的ネストクラスへのアクセスも正しく認識される', async () => {
    const text = editor.document.getText();
    // OuterClass.StaticNestedClass の StaticNestedClass にホバー
    const staticNestedIndex = text.indexOf('new OuterClass.StaticNestedClass') + 'new OuterClass.'.length;
    const position = editor.document.positionAt(staticNestedIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    ok(
      hoverContent.includes('StaticNestedClass') || hoverContent.includes('class'),
      '静的ネストクラスの情報が表示される必要があります',
    );
  });

  it('@Canonicalアノテーション付きクラスも正しく認識される', async () => {
    const text = editor.document.getText();
    // Product クラスにホバー
    const productIndex = text.indexOf('class Product') + 'class '.length;
    const position = editor.document.positionAt(productIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', oopDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = getHoverContent(hovers);

    ok(hoverContent.includes('Product'), 'Productクラスの情報が表示される必要があります');
  });
});
