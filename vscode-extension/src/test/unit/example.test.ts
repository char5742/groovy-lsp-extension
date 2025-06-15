// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as assert from 'node:assert/strict';

describe('Unit Test Example', () => {
  it('Simple arithmetic test', () => {
    assert.strictEqual(1 + 1, 2);
  });

  it('String test', () => {
    const message = 'Hello, World!';
    assert.ok(message.includes('World'));
  });
});
