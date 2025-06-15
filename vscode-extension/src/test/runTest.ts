import * as path from 'node:path';
import { runTests } from '@vscode/test-electron';

async function main() {
  try {
    // Extension Manifest package.jsonを含むフォルダ
    const extensionDevelopmentPath = path.resolve(__dirname, '../../');

    // 拡張機能テストスクリプトへのパス
    const extensionTestsPath = path.resolve(__dirname, './suite/index');

    // テスト実行オプション
    const options = {
      extensionDevelopmentPath,
      extensionTestsPath,
      launchArgs: ['--disable-dev-shm-usage', '--no-sandbox'],
    };

    // ヘッドレス環境での実行を検出
    if (!process.env.DISPLAY && process.platform === 'linux') {
      console.log('Running in headless environment, adding --disable-gpu flag');
      options.launchArgs.push('--disable-gpu');
    }

    // VS Codeをダウンロードし、解凍して統合テストを実行
    await runTests(options);

    // 明示的に成功を通知してプロセスを終了
    console.log('Tests completed successfully');
    process.exit(0);
  } catch (err) {
    console.error('Failed to run tests');
    process.exit(1);
  }
}

main();
