import * as path from 'node:path';
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
  const { testSuitePath, timeout = 60000, description } = options;

  try {
    // Extension Manifest package.jsonを含むフォルダ
    const extensionDevelopmentPath = path.resolve(__dirname, '../../');

    // テストスクリプトへのパス
    const extensionTestsPath = path.resolve(__dirname, testSuitePath);

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
      ],
    };

    // ヘッドレス環境での実行を検出
    if (!process.env.DISPLAY && process.platform === 'linux') {
      console.log('Running in headless environment, adding --disable-gpu flag');
      runOptions.launchArgs.push('--disable-gpu');
    }

    // タイムアウト付きでテストを実行
    const testPromise = runTests(runOptions);
    const timeoutPromise = new Promise<never>((_, reject) => {
      setTimeout(() => reject(new Error(`Test execution timeout after ${timeout / 1000} seconds`)), timeout);
    });

    // VS Codeをダウンロードし、解凍してテストを実行
    await Promise.race([testPromise, timeoutPromise]);
  } catch (err) {
    console.error(`Failed to run ${description}:`, err);
    process.exit(1);
  }
}

/**
 * メイン関数のラッパー
 */
export function createTestRunner(options: TestRunnerOptions): void {
  runTestSuite(options)
    .then(() => {
      console.log(`${options.description} completed successfully`);
      process.exit(0);
    })
    .catch((err) => {
      console.error(`${options.description} execution failed:`, err);
      process.exit(1);
    });
}
