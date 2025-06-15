import { createTestRunner } from './runTestBase';

// 統合テストを実行
createTestRunner({
  testSuitePath: './integration/index',
  timeout: 60000,
  description: 'All integration tests',
});
