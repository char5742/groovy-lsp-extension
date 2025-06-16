import { createTestRunner } from './runTestBase.ts';

// ユニットテストを実行
createTestRunner({
  testSuitePath: './suite/index',
  timeout: 120000,
  description: 'All tests',
});
