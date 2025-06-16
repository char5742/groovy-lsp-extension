import { createTestRunner } from './runTestBase.ts';

// ユニットテストを実行
createTestRunner({
  testSuitePath: './suite/index',
  timeout: 200000, // 200秒に増やして余裕を持たせる
  description: 'All tests',
});
