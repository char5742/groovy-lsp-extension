import * as path from 'node:path';
import { runTests } from '@vscode/test-electron';

async function main() {
  try {
    // Extension Manifest package.jsonを含むフォルダ
    const extensionDevelopmentPath = path.resolve(__dirname, '../../');

    // 拡張機能テストスクリプトへのパス
    const extensionTestsPath = path.resolve(__dirname, './suite/index');

    // VS Codeをダウンロードし、解凍して統合テストを実行
    await runTests({ extensionDevelopmentPath, extensionTestsPath });
  } catch (err) {
    console.error('Failed to run tests');
    process.exit(1);
  }
}

main();
