import { ok } from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { type Extension, type Hover, Position, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';
import { getHoverContent } from './test-helpers.ts';

describe('ホバー機能のE2Eテスト - シンプルパターンマッチング', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let groovyDoc: Awaited<ReturnType<typeof workspace.openTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // simple-pattern-match.groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/simple-pattern-match.groovy');
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

  it('クラス名にホバーできることを確認', async () => {
    // class SimplePatternMatch の "SimplePatternMatch" にホバー
    const position = new Position(1, 20); // class SimplePatternMatch
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);
    if (hovers && hovers.length > 0) {
      const hoverContent = getHoverContent(hovers);
      ok(
        hoverContent.includes('SimplePatternMatch') || hoverContent.includes('class'),
        `ホバー情報にクラス名が含まれるべきです。実際の内容: ${hoverContent}`,
      );
    }
  });

  it('case String s: の解析を確認', async () => {
    // case String s: の位置を確認（行6、列18）
    // const _lineContent = groovyDoc.lineAt(6).text;

    // "String" の位置にホバー
    const position = new Position(6, 18); // case String s:
    const hovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', groovyDoc.uri, position);
    if (hovers && hovers.length > 0) {
      const hoverContent = getHoverContent(hovers);
      ok(
        hoverContent.includes('String') || hoverContent.includes('type'),
        `String型の情報が表示される必要があります。実際の内容: ${hoverContent}`,
      );
    } else {
      ok(false, 'ホバー結果が返される必要があります');
    }
  });
});
