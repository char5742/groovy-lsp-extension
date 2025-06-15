import * as path from 'node:path';
import { glob } from 'glob';
import Mocha from 'mocha';

export function run(): Promise<void> {
  // mochaインスタンスを作成
  const mocha = new Mocha({
    ui: 'bdd',
    timeout: 60000,
    color: true,
  });

  const testsRoot = path.resolve(__dirname, '.');

  return new Promise((resolve, reject) => {
    glob('**/*.test.js', { cwd: testsRoot }, (err, files) => {
      if (err) {
        return reject(err);
      }

      // .spec.jsファイルも含める（Bunテスト用）
      glob('**/*.spec.js', { cwd: testsRoot }, (err2, specFiles) => {
        if (err2) {
          return reject(err2);
        }

        // ファイルをmochaに追加
        for (const f of [...files, ...specFiles]) {
          mocha.addFile(path.resolve(testsRoot, f));
        }

        try {
          // テストを実行
          mocha.run((failures) => {
            if (failures > 0) {
              reject(new Error(`${failures} tests failed.`));
            } else {
              resolve();
            }
          });
        } catch (err) {
          console.error(err);
          reject(err);
        }
      });
    });
  });
}
