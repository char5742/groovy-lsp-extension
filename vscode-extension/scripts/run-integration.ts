import { tmpdir } from 'node:os';
import path from 'node:path';
import { runTests } from '@vscode/test-electron';

async function main() {
  try {
    // 拡張機能のディレクトリパスを取得
    const extensionDevelopmentPath = path.resolve(__dirname, '../..');

    // テストスクリプトのパス
    const extensionTestsPath = path.resolve(__dirname, '../out/test/integration');

    // VS Codeのバージョン
    const vscodeVersion = process.env.VSCODE_VERSION ?? 'stable';

    // テスト用の一時ディレクトリ
    const userDataDir = path.join(tmpdir(), 'vscode-groovy-lsp-test');

    // テストの実行
    await runTests({
      version: vscodeVersion,
      extensionDevelopmentPath,
      extensionTestsPath,
      launchArgs: [
        '--disable-telemetry',
        `--user-data-dir=${userDataDir}`,
        '--disable-workspace-trust',
        '--disable-extensions', // 他の拡張機能を無効化
      ],
    });
  } catch (err) {
    console.error('テストの実行に失敗しました:', err);
    process.exit(1);
  }
}

main();
