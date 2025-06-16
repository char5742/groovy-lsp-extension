import { ok, strictEqual } from 'node:assert/strict';

describe('Unit Test Example', () => {
  it('Simple arithmetic test', () => {
    strictEqual(1 + 1, 2);
  });

  it('String test', () => {
    const message = 'Hello, World!';
    ok(message.includes('World'));
  });
});
