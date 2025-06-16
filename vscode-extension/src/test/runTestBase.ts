import { resolve } from 'node:path';
import { runTests } from '@vscode/test-electron';

/**
 * テスト実行のベース設定
 */
export interface TestRunnerOptions {
  /** テストスイートのパス（indexファイルへの相対パス） */
  testSuitePath: string;
  /** タイムアウト時間（ミリ秒） */
  timeout?: number;
  /** テストの説明 */
  description: string;
}

/**
 * 共通のテスト実行関数
 */
export async function runTestSuite(options: TestRunnerOptions): Promise<void> {
  const { testSuitePath, timeout = 60000 } = options;

  try {
    // Extension Manifest package.jsonを含むフォルダ
    const extensionDevelopmentPath = resolve(__dirname, '../../');

    // テストスクリプトへのパス
    const extensionTestsPath = resolve(__dirname, testSuitePath);

    // コマンドライン引数を取得して渡す
    const extraArgs = process.argv.slice(2);

    // テスト実行オプション
    const runOptions = {
      extensionDevelopmentPath,
      extensionTestsPath,
      launchArgs: [
        '--disable-dev-shm-usage',
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-extensions',
        '--disable-gpu-sandbox',
        ...extraArgs, // コマンドライン引数を追加
      ],
    };

    // ヘッドレス環境での実行を検出
    if (!process.env.DISPLAY && process.platform === 'linux') {
      runOptions.launchArgs.push('--disable-gpu');
    }

    // タイムアウト付きでテストを実行
    const testPromise = runTests(runOptions);
    const timeoutPromise = new Promise<never>((_, reject) => {
      setTimeout(() => reject(new Error(`Test execution timeout after ${timeout / 1000} seconds`)), timeout);
    });

    // VS Codeをダウンロードし、解凍してテストを実行
    await Promise.race([testPromise, timeoutPromise]);
  } catch (_err) {
    process.exit(1);
  }
}

/**
 * メイン関数のラッパー
 */
export function createTestRunner(options: TestRunnerOptions): void {
  runTestSuite(options)
    .then(() => {
      process.exit(0);
    })
    .catch((_err) => {
      process.exit(1);
    });
}
