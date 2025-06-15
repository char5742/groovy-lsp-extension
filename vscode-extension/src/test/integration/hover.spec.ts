import { afterEach, describe, expect, test } from 'bun:test';
import * as vscode from 'vscode';
import { closeDoc, getHoverAt, openDoc } from '../test-utils/lsp';

describe('Hover機能のテスト', () => {
  let doc: vscode.TextDocument;

  afterEach(async () => {
    if (doc) {
      await closeDoc(doc);
    }
  });

  test('メソッド名にホバーすると型情報が表示される', async () => {
    const code = `
      class Example {
        String getMessage() {
          return "Hello, World!"
        }
      }
      
      def example = new Example()
      example.getMessage()
    `;

    doc = await openDoc(code, 'groovy');

    // getMessageメソッドの位置でホバー
    const position = new vscode.Position(7, 14); // "getMessage"の位置
    const hovers = await getHoverAt(doc, position);

    expect(hovers).toBeDefined();
    expect(hovers.length).toBeGreaterThan(0);

    // ホバー情報にString型が含まれることを確認
    const hoverContent = hovers[0].contents[0];
    if (typeof hoverContent === 'object' && 'value' in hoverContent) {
      expect(hoverContent.value).toMatch(/String/);
    }
  });

  test('Spockテストのwhereブロックでホバーが機能する', async () => {
    const code = `
      import spock.lang.Specification
      
      class MathSpec extends Specification {
        def "maximum of two numbers"() {
          expect:
          Math.max(a, b) == c
          
          where:
          a | b | c
          1 | 3 | 3
          7 | 4 | 7
        }
      }
    `;

    doc = await openDoc(code, 'groovy');

    // Math.maxの位置でホバー
    const position = new vscode.Position(5, 10); // "Math.max"の位置
    const hovers = await getHoverAt(doc, position);

    expect(hovers).toBeDefined();
    expect(hovers.length).toBeGreaterThan(0);
  });

  test('Groovyのクロージャでホバーが機能する', async () => {
    const code = `
      def numbers = [1, 2, 3, 4, 5]
      def doubled = numbers.collect { it * 2 }
    `;

    doc = await openDoc(code, 'groovy');

    // collectメソッドの位置でホバー
    const position = new vscode.Position(1, 28); // "collect"の位置
    const hovers = await getHoverAt(doc, position);

    expect(hovers).toBeDefined();
    expect(hovers.length).toBeGreaterThan(0);
  });
});
