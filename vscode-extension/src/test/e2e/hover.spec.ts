import { ok } from 'node:assert/strict';
import { type Hover, Position, type TextDocument, commands, extensions, window } from 'vscode';
import { closeDoc, openDoc } from '../test-utils/lsp.ts';

describe('ホバー機能のテスト', () => {
  let doc: TextDocument;

  before(async () => {
    // 拡張機能が正しくアクティベートされているか確認
    const extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }
    // LSPサーバーが完全に起動するまで待つ
    await new Promise((resolve) => setTimeout(resolve, 3000));
  });

  afterEach(async () => {
    if (doc) {
      await closeDoc(doc);
    }
  });

  it('ホバー時に固定テキスト「Groovy element」が表示される', async () => {
    const code = `class Example {
  def greet() {
    println "Hello, World"
  }
}`;

    doc = await openDoc(code, 'groovy');
    await window.showTextDocument(doc);

    // LSPサーバーがドキュメントを完全に処理するまで待つ
    await new Promise((resolve) => setTimeout(resolve, 2000));

    // カーソルをクラス名の位置に移動
    const position = new Position(0, 10); // Exampleの中央あたり

    // ホバー情報を取得
    const hovers = (await commands.executeCommand('vscode.executeHoverProvider', doc.uri, position)) as Hover[];

    ok(hovers, 'ホバー情報が返されるべきです');
    ok(hovers.length > 0, 'ホバー情報が1つ以上存在するべきです');

    // ホバー内容を確認
    const hover = hovers[0];
    ok(hover.contents, 'ホバーにコンテンツが含まれるべきです');

    // VSCodeのHover APIは contents の中に複数の形式を含む可能性がある
    const foundGroovyElement = checkHoverContents(hover);

    ok(foundGroovyElement, 'ホバー内容に「Groovy element」が含まれるべきです');
  });

  it('メソッド上でもホバー情報が表示される', async () => {
    const code = `class Calculator {
  def add(int a, int b) {
    return a + b
  }
}`;

    doc = await openDoc(code, 'groovy');
    await window.showTextDocument(doc);

    // メソッド名の位置
    const position = new Position(1, 8); // addメソッドの位置

    const hovers = (await commands.executeCommand('vscode.executeHoverProvider', doc.uri, position)) as Hover[];

    ok(hovers && hovers.length > 0, 'メソッド上でもホバー情報が表示されるべきです');
  });

  it('変数上でもホバー情報が表示される', async () => {
    const code = `def message = "Hello"
println message`;

    doc = await openDoc(code, 'groovy');
    await window.showTextDocument(doc);

    // 変数名の位置
    const position = new Position(1, 10); // messageの位置

    const hovers = (await commands.executeCommand('vscode.executeHoverProvider', doc.uri, position)) as Hover[];

    ok(hovers && hovers.length > 0, '変数上でもホバー情報が表示されるべきです');
  });
});

// ホバーコンテンツをチェックするヘルパー関数
function checkHoverContents(hover: Hover): boolean {
  let foundGroovyElement = false;

  if (Array.isArray(hover.contents)) {
    // 配列の各要素をチェック
    for (const content of hover.contents) {
      // VSCode MarkdownString形式
      if (content && typeof content === 'object' && 'value' in content) {
        const value = (content as { value: string }).value;
        // HTMLエンティティが含まれる場合も考慮
        if (value.includes('Groovy element') || value.includes('Groovy&nbsp;element')) {
          foundGroovyElement = true;
          break;
        }
      }
      // プレーンテキスト形式
      else if (typeof content === 'string' && content.includes('Groovy element')) {
        foundGroovyElement = true;
        break;
      }
    }
  }

  return foundGroovyElement;
}
