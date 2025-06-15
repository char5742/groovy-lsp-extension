import { describe, expect, test } from 'bun:test';

describe('Unit Test Example', () => {
  test('Simple arithmetic test', () => {
    expect(1 + 1).toBe(2);
  });

  test('String test', () => {
    const message = 'Hello, World!';
    expect(message).toContain('World');
  });
});
