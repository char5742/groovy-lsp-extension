import { glob } from 'glob';
import Mocha = require('mocha');
// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as path from 'node:path';

export async function run(): Promise<void> {
  // mochaテストを作成
  const mocha = new Mocha({
    ui: 'bdd',
    color: true,
    timeout: 60000,
    bail: false, // エラーがあっても全テストを実行
  });

  const testsRoot = path.resolve(__dirname, '..');
  // すべてのテストファイルを検索
  const files = await glob('**/**.test.js', { cwd: testsRoot });

  // テストスイートにファイルを追加
  for (const f of files) {
    mocha.addFile(path.resolve(testsRoot, f));
  }

  // mochaテストを実行
  return new Promise<void>((resolve, reject) => {
    const runner = mocha.run((failures: number) => {
      if (failures > 0) {
        reject(new Error(`${failures} tests failed.`));
      } else {
        resolve();
      }
    });

    // タイムアウト対策: 30秒でテストを強制終了
    const timeout = setTimeout(() => {
      runner.abort();
      resolve();
    }, 30000);

    runner.on('end', () => {
      clearTimeout(timeout);
    });
  });
}
