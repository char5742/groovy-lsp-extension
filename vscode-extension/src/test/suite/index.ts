import { glob } from 'glob';
import Mocha = require('mocha');
import { resolve } from 'node:path';

export async function run(): Promise<void> {
  // 環境変数からgrepパターンを取得
  const grepPattern = process.env.MOCHA_GREP || undefined;

  // mochaテストを作成
  const mocha = new Mocha({
    ui: 'bdd',
    color: true,
    timeout: 120000,
    bail: false, // エラーがあっても全テストを実行
    grep: grepPattern, // --grepオプションがあれば設定
    reporter: 'spec', // 詳細な出力
  });

  const testsRoot = resolve(__dirname, '..');
  // すべてのテストファイルを検索
  const files = await glob('**/*{.test,.spec}.js', { cwd: testsRoot });

  if (grepPattern) {
    // grep パターンは mocha に既に設定済み
  }

  // テストスイートにファイルを追加
  for (const f of files) {
    mocha.addFile(resolve(testsRoot, f));
  }

  // mochaテストを実行
  return new Promise<void>((resolve, reject) => {
    let isResolved = false;

    const runner = mocha.run((failures: number) => {
      if (!isResolved) {
        isResolved = true;
        if (failures > 0) {
          reject(new Error(`${failures} tests failed.`));
        } else {
          resolve();
        }
      }
    });

    // タイムアウト対策: 300秒でテストを強制終了（多数のテストに対応）
    const timeout = setTimeout(() => {
      if (!isResolved) {
        isResolved = true;
        runner.abort();
        reject(new Error('Test execution timeout after 300 seconds'));
      }
    }, 300000);

    runner.on('end', () => {
      clearTimeout(timeout);
      // すべてのテストが完了したら、少し待ってからプロセスを終了
      setTimeout(() => {
        if (!isResolved) {
          isResolved = true;
          resolve();
        }
      }, 1000);
    });

    // 各テストのタイムアウトエラーをキャッチ
    runner.on('fail', (_test, err) => {
      if (err.message?.includes('timeout')) {
        // タイムアウトエラーはすでにログに記録されている
      }
    });
  });
}
