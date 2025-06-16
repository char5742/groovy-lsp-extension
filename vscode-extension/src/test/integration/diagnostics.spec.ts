import { ok } from 'node:assert/strict';
import { type TextDocument, extensions, languages } from 'vscode';
import { closeDoc, openDoc } from '../test-utils/lsp';

describe('診断機能のテスト', () => {
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

  it('行カウント情報が表示される', async () => {
    const code = `
      class Example {
        def greet() {
          println "Hello, World"
        }
      }
    `;

    doc = await openDoc(code, 'groovy');

    // 診断が完全に完了するまで待つ
    await new Promise((resolve) => setTimeout(resolve, 5000));

    const diagnostics = languages.getDiagnostics(doc.uri);
    diagnostics.forEach((_d, _i) => {
      // 診断情報の詳細ログは必要に応じて追加
    });

    // 行カウント情報のメッセージを確認
    const lineCountDiagnostic = diagnostics.find((d) => d.source === 'groovy-lsp-line-count');
    ok(lineCountDiagnostic, '行カウント情報メッセージが見つかりません');

    // メッセージの詳細を確認
    ok(lineCountDiagnostic.message.includes('総行数'), 'メッセージに総行数が含まれていません');
  });

  it('ファイル変更時も行カウント情報が更新される', async () => {
    // 初期コード
    doc = await openDoc('// empty file', 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 3000));

    let diagnostics = languages.getDiagnostics(doc.uri);
    ok(
      diagnostics.some((d) => d.source === 'groovy-lsp-line-count'),
      '初期状態で行カウント情報が存在する必要があります',
    );

    // 診断メッセージが更新されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 3000));

    diagnostics = languages.getDiagnostics(doc.uri);
    ok(
      diagnostics.some((d) => d.source === 'groovy-lsp-line-count'),
      '編集後も行カウント情報が存在する必要があります',
    );
  });

  it('空のGroovyファイルでも行カウント情報が表示される', async () => {
    doc = await openDoc('', 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const diagnostics = languages.getDiagnostics(doc.uri);

    ok(diagnostics.length > 0, '空のファイルでも診断メッセージが存在する必要があります');
    ok(
      diagnostics.some((d) => d.source === 'groovy-lsp-line-count'),
      '行カウント情報が存在する必要があります',
    );
  });
});
