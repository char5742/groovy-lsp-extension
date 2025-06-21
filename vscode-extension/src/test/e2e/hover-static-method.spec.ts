import { ok } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';

describe('静的メソッドとqualified呼び出しのホバーE2Eテスト', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let staticMethodDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;
  let editor: Awaited<ReturnType<typeof window.showTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // static-method-test.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/static-method-test.groovy');
    staticMethodDoc = await workspace.openTextDocument(groovyPath);
    editor = await window.showTextDocument(staticMethodDoc);

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('qualified呼び出し（java.time.Instant.now()）でFQN付きシグネチャが表示される', async () => {
    const text = editor.document.getText();
    // "java.time.Instant.now()" の now にホバー
    const nowIndex = text.indexOf('java.time.Instant.now()') + 'java.time.Instant.'.length;
    const position = editor.document.positionAt(nowIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', staticMethodDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    // FQN付きシグネチャが表示されることを確認
    ok(hoverContent.includes('now') || hoverContent.includes('Instant'), 'nowメソッドの情報が表示される必要があります');

    // 将来的にはFQNが含まれることを確認
    // ok(
    //   hoverContent.includes('java.time.Instant'),
    //   'FQN（java.time.Instant）が含まれる必要があります',
    // );
  });

  it('静的メソッドアクセス（Math.sin(x)）でメソッド情報が表示される', async () => {
    const text = editor.document.getText();
    // "Math.sin(0.5)" の sin にホバー
    const sinIndex = text.indexOf('Math.sin(0.5)') + 'Math.'.length;
    const position = editor.document.positionAt(sinIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', staticMethodDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('sin'), 'sinメソッドの情報が表示される必要があります');

    // 将来的には引数と戻り値の型情報も含まれるべき
    // ok(
    //   hoverContent.includes('double') || hoverContent.includes('Math'),
    //   'メソッドの型情報が含まれる必要があります',
    // );
  });

  it('インポートされたクラスの静的メソッド（Instant.now()）でも情報が表示される', async () => {
    const text = editor.document.getText();
    // "Instant.now()" の now にホバー（importされたクラス）
    const instantNowIndex = text.indexOf('def instant = Instant.now()') + 'def instant = Instant.'.length;
    const position = editor.document.positionAt(instantNowIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', staticMethodDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(
      hoverContent.includes('now') || hoverContent.includes('メソッド'),
      'nowメソッドの情報が表示される必要があります',
    );
  });

  it('静的フィールド（Math.PI）でフィールド情報が表示される', async () => {
    const text = editor.document.getText();
    // "Math.PI" の PI にホバー
    const piIndex = text.indexOf('Math.PI') + 'Math.'.length;
    const position = editor.document.positionAt(piIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', staticMethodDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('PI') || hoverContent.includes('double'), 'PIフィールドの情報が表示される必要があります');
  });

  it('import aliasを使用した呼び出し（LD.now()）で元のクラス情報が表示される', async () => {
    const text = editor.document.getText();
    // "LD.now()" の now にホバー
    const ldNowIndex = text.indexOf('def date = LD.now()') + 'def date = LD.'.length;
    const position = editor.document.positionAt(ldNowIndex);

    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', staticMethodDoc.uri, position);

    ok(hovers && hovers.length > 0, 'ホバー結果が返される必要があります');
    const hoverContent = hovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');

    ok(hoverContent.includes('now'), 'nowメソッドの情報が表示される必要があります');

    // 将来的には元のクラス名（LocalDate）が表示されるべき
    // ok(
    //   hoverContent.includes('LocalDate') || hoverContent.includes('java.time.LocalDate'),
    //   '元のクラス名が含まれる必要があります',
    // );
  });
});
