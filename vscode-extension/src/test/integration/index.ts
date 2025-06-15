// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as path from 'node:path';
import { glob } from 'glob';
import Mocha = require('mocha');

export function run(): Promise<void> {
  // mochaインスタンスを作成
  const mocha = new Mocha({
    ui: 'bdd',
    timeout: 60000,
    color: true,
  });

  const testsRoot = path.resolve(__dirname, '.');

  return new Promise((resolve, reject) => {
    // ファイルを非同期で読み込む
    Promise.all([glob('**/*.test.js', { cwd: testsRoot }), glob('**/*.spec.js', { cwd: testsRoot })])
      .then(([testFiles, specFiles]) => {
        // ファイルをmochaに追加
        for (const f of [...testFiles, ...specFiles]) {
          mocha.addFile(path.resolve(testsRoot, f));
        }

        // テストを実行
        mocha.run((failures) => {
          if (failures > 0) {
            reject(new Error(`${failures} tests failed.`));
          } else {
            resolve();
          }
        });
      })
      .catch((err) => {
        reject(err);
      });
  });
}
