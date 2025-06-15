// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as assert from 'node:assert/strict';
// biome-ignore lint/style/noNamespaceImport: VSCode APIを使用
// biome-ignore lint/correctness/noUndeclaredDependencies: VSCodeが提供
import * as vscode from 'vscode';
import { closeDoc, getLanguageClient, openDoc } from '../test-utils/lsp.ts';

// 診断を待つラッパー関数
async function waitForDiagnostics(doc: vscode.TextDocument): Promise<vscode.Diagnostic[]> {
  // 診断が更新されるまで待つ（最大10秒）
  const maxWaitTime = 10000;
  const checkInterval = 100;
  let totalWaitTime = 0;

  while (totalWaitTime < maxWaitTime) {
    const diagnostics = vscode.languages.getDiagnostics(doc.uri);
    if (diagnostics.length > 0) {
      return diagnostics;
    }
    await new Promise((resolve) => setTimeout(resolve, checkInterval));
    totalWaitTime += checkInterval;
  }
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

    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // LSPクライアントの確認
    const client = await getLanguageClient();

    if (client) {
      // クライアントが利用可能であることを確認
    }

    const code = `
      class Example {
        def greet() {
          println "Hello, World"
        }
      }
    `;

    doc = await openDoc(code, 'groovy');

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
