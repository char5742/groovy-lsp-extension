import { glob } from 'glob';
import Mocha = require('mocha');
import * as path from 'node:path';

export async function run(): Promise<void> {
  // Create the mocha test
  const mocha = new Mocha({
    ui: 'tdd',
    color: true,
    timeout: 60000,
  });

  const testsRoot = path.resolve(__dirname, '..');

  try {
    // Find all test files
    const files = await glob('**/**.test.js', { cwd: testsRoot });

    // Add files to the test suite
    for (const f of files) {
      mocha.addFile(path.resolve(testsRoot, f));
    }

    // Run the mocha test
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
