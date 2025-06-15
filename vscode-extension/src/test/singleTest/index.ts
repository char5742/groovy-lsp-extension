import * as path from 'node:path';
import Mocha = require('mocha');

export function run(): Promise<void> {
  const mocha = new Mocha({
    ui: 'bdd',
    timeout: 60000,
    color: true,
  });

  const testsRoot = path.resolve(__dirname, '..');

  return new Promise((resolve, reject) => {
    // 診断機能のテストファイルのみを追加
    mocha.addFile(path.resolve(testsRoot, 'integration/diagnostics.spec.js'));

    try {
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
}
