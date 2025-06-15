import * as assert from 'node:assert';

describe('Unit Test Example', () => {
  it('Simple arithmetic test', () => {
    assert.strictEqual(1 + 1, 2);
  });

  it('String test', () => {
    const message = 'Hello, World!';
    assert.ok(message.includes('World'));
  });
});
