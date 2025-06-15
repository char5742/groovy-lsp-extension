import { createTestRunner } from './runTestBase.ts';

// ユニットテストを実行
createTestRunner({
  testSuitePath: './suite/index',
  timeout: 45000,
  description: 'All tests',
});
