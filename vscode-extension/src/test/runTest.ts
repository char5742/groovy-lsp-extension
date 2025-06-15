import { createTestRunner } from './runTestBase';

// ユニットテストを実行
createTestRunner({
  testSuitePath: './suite/index',
  timeout: 45000,
  description: 'All tests',
});
