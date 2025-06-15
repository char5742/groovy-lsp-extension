import { createTestRunner } from './runTestBase';

// 診断機能のテストのみを実行
createTestRunner({
  testSuitePath: './singleTest/index',
  timeout: 60000,
  description: 'Diagnostics test',
});
