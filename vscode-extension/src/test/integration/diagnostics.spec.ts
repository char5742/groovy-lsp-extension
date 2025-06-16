// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as assert from 'node:assert/strict';
// biome-ignore lint/style/noNamespaceImport: VSCode APIを使用
// biome-ignore lint/correctness/noUndeclaredDependencies: VSCodeが提供
import * as vscode from 'vscode';
import { closeDoc, openDoc } from '../test-utils/lsp';

describe('診断機能のテスト', () => {
  let doc: vscode.TextDocument;

  before(async () => {
    // 拡張機能が正しくアクティベートされているか確認
    const extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
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

  it('行カウント情報が表示される', async () => {
    const code = `
      class Example {
        def greet() {
          println "Hello, World"
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    console.log('ドキュメントURI:', doc.uri.toString());

    // 診断が完全に完了するまで待つ
    await new Promise((resolve) => setTimeout(resolve, 5000));

    const diagnostics = vscode.languages.getDiagnostics(doc.uri);
    console.log('診断数:', diagnostics.length);
    diagnostics.forEach((d, i) => {
      console.log(`診断[${i}]:`, {
        message: d.message,
        source: d.source,
        severity: d.severity,
        range: `${d.range.start.line}:${d.range.start.character}-${d.range.end.line}:${d.range.end.character}`,
      });
    });

    // 行カウント情報のメッセージを確認
    const lineCountDiagnostic = diagnostics.find((d) => d.source === 'groovy-lsp-line-count');
    assert.ok(lineCountDiagnostic, '行カウント情報メッセージが見つかりません');

    // メッセージの詳細を確認
    assert.ok(lineCountDiagnostic.message.includes('総行数'), 'メッセージに総行数が含まれていません');
    assert.strictEqual(
      lineCountDiagnostic.severity,
      vscode.DiagnosticSeverity.Information,
      'INFORMATIONレベルである必要があります',
    );
    assert.strictEqual(lineCountDiagnostic.range.start.line, 0, '開始行は0である必要があります');
    assert.strictEqual(lineCountDiagnostic.range.start.character, 0, '開始列は0である必要があります');
  });

  it('ファイル変更時も行カウント情報が更新される', async () => {
    // 初期コード
    doc = await openDoc('// empty file', 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 3000));

    let diagnostics = vscode.languages.getDiagnostics(doc.uri);
    assert.ok(
      diagnostics.some((d) => d.source === 'groovy-lsp-line-count'),
      '初期状態で行カウント情報が存在する必要があります',
    );

    // ファイルを編集
    const edit = new vscode.WorkspaceEdit();
    edit.replace(doc.uri, new vscode.Range(0, 0, 0, 13), 'class NewClass {}');
    await vscode.workspace.applyEdit(edit);

    // 診断メッセージが更新されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 3000));

    diagnostics = vscode.languages.getDiagnostics(doc.uri);
    assert.ok(
      diagnostics.some((d) => d.source === 'groovy-lsp-line-count'),
      '編集後も行カウント情報が存在する必要があります',
    );
  });

  it('空のGroovyファイルでも行カウント情報が表示される', async () => {
    doc = await openDoc('', 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const diagnostics = vscode.languages.getDiagnostics(doc.uri);

    assert.ok(diagnostics.length > 0, '空のファイルでも診断メッセージが存在する必要があります');
    assert.ok(
      diagnostics.some((d) => d.source === 'groovy-lsp-line-count'),
      '行カウント情報が存在する必要があります',
    );
  });
});
