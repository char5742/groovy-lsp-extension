import { ok, strictEqual } from 'node:assert/strict';
import { type Diagnostic, type TextDocument, extensions, languages } from 'vscode';
import { closeDoc, openDoc } from '../test-utils/lsp.ts';

// 診断を待つラッパー関数
async function waitForDiagnostics(doc: TextDocument, expectedCount?: number): Promise<Diagnostic[]> {
  const maxWaitTime = 10000;
  const checkInterval = 100;
  let totalWaitTime = 0;

  while (totalWaitTime < maxWaitTime) {
    const diagnostics = languages.getDiagnostics(doc.uri);
    if (expectedCount !== undefined) {
      if (diagnostics.length >= expectedCount) {
        return diagnostics;
      }
    } else if (diagnostics.length > 0) {
      return diagnostics;
    }
    await new Promise((resolve) => setTimeout(resolve, checkInterval));
    totalWaitTime += checkInterval;
  }
  return languages.getDiagnostics(doc.uri);
}

describe('括弧の対応チェック機能のテスト', () => {
  let doc: TextDocument;

  before(async () => {
    // 拡張機能が正しくアクティベートされているか確認
    const extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }
    // LSPサーバーが完全に起動するまで待つ
    await new Promise((resolve) => setTimeout(resolve, 3000));
  });

  afterEach(async () => {
    if (doc) {
      await closeDoc(doc);
    }
  });

  it('正しい括弧のペアではエラーが表示されない', async () => {
    const code = `
      class Example {
        def method() {
          def list = [1, 2, 3]
          def map = ['key': 'value']
          def result = calculate(10 * (5 + 3))
          return result
        }
        
        def calculate(int value) {
          return value * 2
        }
      }
    `;

    doc = await openDoc(code, 'groovy');

    // 診断が完全に完了するまで待つ
    await new Promise((resolve) => setTimeout(resolve, 5000));

    const diagnostics = languages.getDiagnostics(doc.uri);
    diagnostics.forEach((_d, _i) => {
      // 診断情報の詳細ログは必要に応じて追加
    });

    const bracketErrors = diagnostics.filter((d) => d.source === 'groovy-lsp-bracket-validation');

    strictEqual(bracketErrors.length, 0, '正しい括弧のペアではエラーが表示されないはずです');
  });

  it('開き括弧が多い場合にエラーが表示される', async () => {
    const code = `
      class Example {
        def method() {
          def list = [1, 2, 3  // 閉じ括弧がない
          def result = calculate(10 * (5 + 3)
          return result
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc, 2);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes') ||
        d.message.toLowerCase().includes('unclosed') ||
        d.message.toLowerCase().includes('missing'),
    );

    ok(bracketErrors.length > 0, '開き括弧が多い場合はエラーが表示されるはずです');
  });

  it('閉じ括弧が多い場合にエラーが表示される', async () => {
    const code = `
      class Example {
        def method() {
          def list = [1, 2, 3]]  // 余分な閉じ括弧
          def result = calculate(10 * (5 + 3)))
          return result
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc, 2);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes') ||
        d.message.toLowerCase().includes('unexpected') ||
        d.message.toLowerCase().includes('extra'),
    );

    ok(bracketErrors.length > 0, '閉じ括弧が多い場合はエラーが表示されるはずです');
  });

  it('異なる種類の括弧の不一致でエラーが表示される', async () => {
    const code = `
      class Example {
        def method() {
          def list = [1, 2, 3)  // 角括弧と丸括弧の不一致
          def map = {'key': 'value']  // 波括弧と角括弧の不一致
          return null
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc, 2);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes') ||
        d.message.toLowerCase().includes('mismatch') ||
        d.message.toLowerCase().includes('expected'),
    );

    ok(bracketErrors.length > 0, '異なる種類の括弧の不一致でエラーが表示されるはずです');
  });

  it('ネストされた括弧が正しく対応している場合はエラーが表示されない', async () => {
    const code = `
      class Example {
        def method() {
          def complex = [[1, 2, [3, 4]], [5, [6, 7, [8, 9]]]]
          def calculation = ((10 + 5) * (3 + (2 * 4)))
          def map = [
            'key1': ['nested': [1, 2, 3]],
            'key2': ['values': [4, 5, 6]]
          ]
          return complex
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes'),
    );

    strictEqual(bracketErrors.length, 0, 'ネストされた括弧が正しい場合はエラーが表示されないはずです');
  });

  it('文字列内の括弧は無視される', async () => {
    const code = `
      class Example {
        def method() {
          def text = "This has (unmatched brackets] and {more}"
          def regex = /\[.*\]/
          def multiline = '''
            This also has [unmatched
            brackets) in the string
          '''
          return text
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes'),
    );

    strictEqual(bracketErrors.length, 0, '文字列内の括弧は無視されるはずです');
  });

  it('コメント内の括弧は無視される', async () => {
    const code = `
      class Example {
        def method() {
          // This comment has (unmatched brackets]
          /* This block comment
             also has {unmatched
             brackets) */
          def value = 42
          return value
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes'),
    );

    strictEqual(bracketErrors.length, 0, 'コメント内の括弧は無視されるはずです');
  });

  it('Groovy特有の構文での括弧チェック', async () => {
    const code = `
      class Example {
        def method() {
          // クロージャ
          def closure = { param ->
            println param
          }
          
          // GString
          def name = "World"
          def greeting = "Hello \${name}"
          
          // 範囲演算子
          def range = 1..10
          
          // エルビス演算子とセーフナビゲーション
          def result = obj?.method() ?: defaultValue
          
          return closure
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes'),
    );

    strictEqual(bracketErrors.length, 0, 'Groovy特有の構文でも正しい括弧チェックが行われるはずです');
  });

  it('複雑なSpockテストでの括弧チェック', async () => {
    const code = `
      class ExampleSpec extends Specification {
        def "複雑なデータテーブルでのテスト"() {
          given:
          def calculator = new Calculator()
          
          when:
          def result = calculator.calculate(a, b, operation)
          
          then:
          result == expected
          
          where:
          a  | b  | operation | expected
          10 | 5  | "+"       | 15
          20 | 4  | "-"       | 16
          [1, 2] | [3, 4] | "concat" | [1, 2, 3, 4]
          ['a': 1] | ['b': 2] | "merge" | ['a': 1, 'b': 2]
        }
      }
    `;

    doc = await openDoc(code, 'groovy');
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const diagnostics = await waitForDiagnostics(doc);
    const bracketErrors = diagnostics.filter(
      (d) =>
        d.message.includes('括弧') ||
        d.message.toLowerCase().includes('bracket') ||
        d.message.toLowerCase().includes('parenthes'),
    );

    strictEqual(bracketErrors.length, 0, 'Spockテストの複雑な構文でも括弧チェックが正しく動作するはずです');
  });
});
