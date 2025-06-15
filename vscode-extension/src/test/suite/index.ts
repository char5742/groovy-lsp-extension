import { glob } from 'glob';
import Mocha = require('mocha');
import * as path from 'node:path';

export async function run(): Promise<void> {
  // mochaテストを作成
  const mocha = new Mocha({
    ui: 'bdd',
    color: true,
    timeout: 60000,
  });

  const testsRoot = path.resolve(__dirname, '..');

  try {
    // すべてのテストファイルを検索
    const files = await glob('**/**.test.js', { cwd: testsRoot });

    // テストスイートにファイルを追加
    for (const f of files) {
      mocha.addFile(path.resolve(testsRoot, f));
    }

    // mochaテストを実行
    return new Promise<void>((resolve, reject) => {
      mocha.run((failures: number) => {
        if (failures > 0) {
          reject(new Error(`${failures} tests failed.`));
        } else {
          resolve();
        }
      });
    });
  } catch (err) {
    console.error(err);
    throw err;
  }
}
