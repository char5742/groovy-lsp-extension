import * as assert from 'node:assert';
import * as vscode from 'vscode';
import { closeDoc, getDiagnostics, getLanguageClient, openDoc } from '../test-utils/lsp';

// 診断を待つラッパー関数
async function waitForDiagnostics(doc: vscode.TextDocument): Promise<vscode.Diagnostic[]> {
  // 診断が更新されるまで待つ（最大10秒）
  const maxWaitTime = 10000;
  const checkInterval = 100;
  let totalWaitTime = 0;

  while (totalWaitTime < maxWaitTime) {
    const diagnostics = vscode.languages.getDiagnostics(doc.uri);
    if (diagnostics.length > 0) {
      console.log(`診断を取得しました: ${diagnostics.length}件`);
      return diagnostics;
    }
    await new Promise((resolve) => setTimeout(resolve, checkInterval));
    totalWaitTime += checkInterval;
  }

  console.log('診断が取得できませんでした');
  return [];
}

describe('診断機能のテスト', () => {
  let doc: vscode.TextDocument;

  afterEach(async () => {
    if (doc) {
      await closeDoc(doc);
    }
  });

  it('固定メッセージ「Hello from Groovy LSP」が表示される (issue #5)', async () => {
    // 拡張機能が正しくアクティベートされているか確認
    const extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    console.log('拡張機能:', extension ? '存在' : '存在しない');

    if (extension && !extension.isActive) {
      console.log('拡張機能をアクティベート中...');
      await extension.activate();
    }

    // LSPクライアントの確認
    const client = await getLanguageClient();
    console.log('LSPクライアント:', client ? '初期化済み' : '未初期化');

    if (client) {
      console.log('LSPクライアント状態:', await client.state);
    }

    const code = `
      class Example {
        def greet() {
          println "Hello, World"
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    console.log('ドキュメントを開きました:', doc.uri.toString());

    // 少し待機してからテスト
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc);

    // 診断メッセージが存在することを確認
    assert.ok(diagnostics.length > 0, '診断メッセージが存在する必要があります');

    // 固定メッセージの内容を確認
    const helloDiagnostic = diagnostics.find((d) => d.message === 'Hello from Groovy LSP');
    assert.ok(helloDiagnostic, '「Hello from Groovy LSP」メッセージが見つかりません');

    // メッセージの詳細を確認
    assert.strictEqual(
      helloDiagnostic.severity,
      vscode.DiagnosticSeverity.Information,
      'INFORMATIONレベルである必要があります',
    );
    assert.strictEqual(helloDiagnostic.range.start.line, 0, '開始行は0である必要があります');
    assert.strictEqual(helloDiagnostic.range.start.character, 0, '開始列は0である必要があります');
    assert.strictEqual(helloDiagnostic.source, 'groovy-lsp', 'ソースはgroovy-lspである必要があります');
  });

  it('ファイル変更時も固定メッセージが維持される', async () => {
    // 初期コード
    doc = await openDoc('// empty file', 'groovy');
    let diagnostics = await waitForDiagnostics(doc);
    assert.ok(
      diagnostics.some((d) => d.message === 'Hello from Groovy LSP'),
      '初期状態でメッセージが存在する必要があります',
    );

    // ファイルを編集
    const edit = new vscode.WorkspaceEdit();
    edit.replace(doc.uri, new vscode.Range(0, 0, 0, 13), 'class NewClass {}');
    await vscode.workspace.applyEdit(edit);

    // 診断メッセージが更新されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    diagnostics = await waitForDiagnostics(doc);
    assert.ok(
      diagnostics.some((d) => d.message === 'Hello from Groovy LSP'),
      '編集後もメッセージが存在する必要があります',
    );
  });

  it('空のGroovyファイルでも固定メッセージが表示される', async () => {
    doc = await openDoc('', 'groovy');
    const diagnostics = await waitForDiagnostics(doc);

    assert.ok(diagnostics.length > 0, '空のファイルでも診断メッセージが存在する必要があります');
    assert.ok(
      diagnostics.some((d) => d.message === 'Hello from Groovy LSP'),
      '固定メッセージが存在する必要があります',
    );
  });
});
