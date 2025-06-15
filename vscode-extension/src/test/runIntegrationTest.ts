import { createTestRunner } from './runTestBase.ts';

// 統合テストを実行
createTestRunner({
  testSuitePath: './integration/index',
  timeout: 60000,
  description: 'All integration tests',
});
