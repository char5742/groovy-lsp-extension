/**
 * テストメタデータを記述する例
 *
 * このファイルは実際のテストではなく、メタデータの記述方法を示すサンプルです。
 */

// 方法1: JSDocコメントを使用
suite('Example Test Suite', () => {
  /**
   * @scenario ユーザー認証
   * @feature AUTH-001
   * @priority high
   * @prerequisites
   *   - データベースが起動している
   *   - テストユーザーが作成されている
   * @steps
   *   1. ユーザー名とパスワードを入力
   *   2. ログインボタンをクリック
   *   3. レスポンスを待つ
   * @expected
   *   - HTTPステータス200が返される
   *   - 認証トークンが含まれる
   *   - ユーザー情報が正しい
   */
  test('Should authenticate user with valid credentials @auth @smoke', async () => {
    // テスト実装
  });
});

// 方法2: 構造化されたテスト記述
describe('Advanced Test Pattern', () => {
  // テストごとにメタデータオブジェクトを定義
  const testCases = [
    {
      name: 'Should handle concurrent requests @performance @stress',
      scenario: '同時リクエスト処理',
      feature: 'PERF-001',
      maxDuration: 5000, // ms
      setup: () => {
        // セットアップコード
      },
      test: async () => {
        // テスト実装
      },
      teardown: () => {
        // クリーンアップ
      },
    },
  ];

  for (const tc of testCases) {
    test(tc.name, async function () {
      this.timeout(tc.maxDuration);
      if (tc.setup) tc.setup();
      try {
        await tc.test();
      } finally {
        if (tc.teardown) tc.teardown();
      }
    });
  }
});

// 方法3: カスタムテスト関数（別ファイルで定義して使用）
interface ScenarioMetadata {
  name: string;
  feature?: string;
  prerequisites?: string[];
  steps?: string[];
  expected?: string[];
  tags?: string[];
}

// 実際の使用例（decoratorは別ファイルからimport）
// @scenario({
//   name: 'ユーザー登録',
//   feature: 'USER-001',
//   prerequisites: ['DBが起動している'],
//   steps: ['フォームに入力', '送信ボタンクリック'],
//   expected: ['成功メッセージ表示', 'DBに保存']
// })
// test('Should register new user', async () => {
//   // テスト実装
// });
