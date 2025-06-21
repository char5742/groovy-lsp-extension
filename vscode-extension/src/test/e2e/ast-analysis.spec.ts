import { strictEqual } from 'node:assert/strict';
import { DiagnosticSeverity, type TextDocument, commands, extensions, languages } from 'vscode';
import { closeDoc, openDoc } from '../test-utils/lsp.ts';

// テストで使用する待機時間の定数
const WAIT_TIME_MS = 3000;

describe('AST解析機能のテスト', () => {
  let doc: TextDocument;

  before(async function () {
    this.timeout(10000); // before フックのタイムアウトを延長
    // 拡張機能が正しくアクティベートされているか確認
    const extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }
    // LSPサーバーが完全に起動するまで待つ
    await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));
  });

  afterEach(async () => {
    if (doc) {
      await closeDoc(doc);
    }
  });

  describe('構文エラーの検出', () => {
    it('閉じ括弧が不足している場合、構文エラーが検出される', async () => {
      const code = `
        class BrokenClass {
          void method() {
            println "Missing closing brace"
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );

      strictEqual(syntaxErrors.length > 0, true, '構文エラーが検出されるべきです');
      strictEqual(
        syntaxErrors.some(
          (e) => e.message.toLowerCase().includes('unexpected') || e.message.toLowerCase().includes('missing'),
        ),
        true,
        'エラーメッセージに構文エラーの詳細が含まれるべきです',
      );
    });

    it('不正なインポート文がある場合、構文エラーが検出される', async () => {
      const code = `
        import invalid.package.
        
        class TestClass {
          void test() {
            println "test"
          }
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );

      strictEqual(syntaxErrors.length > 0, true, '不正なインポート文に対してエラーが検出されるべきです');
    });

    it('正しい構文の場合、構文エラーが検出されない', async () => {
      const code = `
        package com.example
        
        import java.util.List
        
        class ValidClass {
          String name
          int age
          
          void display() {
            println "Name: \$name, Age: \$age"
          }
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );

      strictEqual(syntaxErrors.length, 0, '正しい構文では構文エラーが検出されないべきです');
    });
  });

  describe('クラス定義の認識', () => {
    it('複数のクラス定義がある場合でも、各クラスが正しく認識される', async () => {
      const code = `
        class FirstClass {
          void method1() {
            println "First"
          }
        }
        
        interface SecondInterface {
          void method2()
        }
        
        enum ThirdEnum {
          VALUE1, VALUE2
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);

      // 構文エラーがないことを確認
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );
      strictEqual(syntaxErrors.length, 0, '複数クラス定義で構文エラーが発生しないべきです');

      // 行カウント情報が存在することで、AST解析が成功したことを間接的に確認
      const lineCountDiagnostic = diagnostics.find((d) => d.source === 'groovy-lsp-line-count');
      strictEqual(lineCountDiagnostic !== undefined, true, 'AST解析が成功し、診断情報が生成されるべきです');
    });
  });

  describe('メソッド定義の認識', () => {
    it('Spockテストメソッドの特殊な名前が正しく認識される', async () => {
      const code = `
        import spock.lang.Specification
        
        class CalculatorSpec extends Specification {
          def "addition should work correctly"() {
            given:
            def calculator = new Calculator()
            
            when:
            def result = calculator.add(2, 3)
            
            then:
            result == 5
          }
          
          def "subtraction should work"() {
            expect:
            5 - 3 == 2
          }
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);

      // Spockテストの特殊な構文でも構文エラーが発生しないことを確認
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );
      strictEqual(syntaxErrors.length, 0, 'Spockテストメソッドで構文エラーが発生しないべきです');
    });

    it('様々なメソッド修飾子が正しく認識される', async () => {
      const code = `
        class MethodTest {
          void publicMethod() {
            println "Public"
          }
          
          private String privateMethod(String name) {
            return "Hello, \$name"
          }
          
          protected static int staticMethod(int x) {
            return x * 2
          }
          
          @Override
          String toString() {
            return "MethodTest"
          }
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);

      // 様々な修飾子を持つメソッドでも構文エラーが発生しないことを確認
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );
      strictEqual(syntaxErrors.length, 0, '様々なメソッド修飾子で構文エラーが発生しないべきです');
    });
  });

  describe('複雑な構文の処理', () => {
    it('クロージャとGStringを含むコードが正しく解析される', async () => {
      const code = `
        class ClosureExample {
          def list = [1, 2, 3, 4, 5]
          
          def processData() {
            def evenNumbers = list.findAll { it % 2 == 0 }
            
            list.each { item ->
              println "Item: \$item"
            }
            
            def multiplier = { factor ->
              return { number -> number * factor }
            }
            
            def triple = multiplier(3)
            println triple(10)
          }
          
          def multilineString = """
            This is a multiline
            GString with \${list.size()} items
          """
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);

      // クロージャやGStringを含む複雑な構文でもエラーが発生しないことを確認
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );
      strictEqual(syntaxErrors.length, 0, 'クロージャとGStringを含むコードで構文エラーが発生しないべきです');
    });

    it('Groovy特有の構文（プロパティアクセス、安全参照演算子など）が正しく解析される', async () => {
      const code = `
        class GroovyFeatures {
          String name
          int age
          
          def testFeatures() {
            // プロパティアクセス
            this.name = "John"
            
            // 安全参照演算子
            def length = name?.length()
            
            // エルビス演算子
            def defaultName = name ?: "Unknown"
            
            // スプレッド演算子
            def numbers = [1, 2, 3]
            def doubled = numbers*.multiply(2)
            
            // 範囲演算子
            def range = 1..10
            
            // 正規表現
            def pattern = ~/[a-zA-Z]+/
            
            // マップリテラル
            def map = [key1: 'value1', key2: 'value2']
          }
        }
      `;

      doc = await openDoc(code, 'groovy');
      await new Promise((resolve) => setTimeout(resolve, WAIT_TIME_MS));

      const diagnostics = languages.getDiagnostics(doc.uri);

      // Groovy特有の構文でもエラーが発生しないことを確認
      const syntaxErrors = diagnostics.filter(
        (d) => d.source === 'groovy-syntax' && d.severity === DiagnosticSeverity.Error,
      );
      strictEqual(syntaxErrors.length, 0, 'Groovy特有の構文で構文エラーが発生しないべきです');
    });
  });

  // テストスイート終了時のクリーンアップ
  after(async () => {
    // 開いているドキュメントをすべて閉じる
    await commands.executeCommand('workbench.action.closeAllEditors');
    // 少し待つ
    await new Promise((resolve) => setTimeout(resolve, 500));
  });
});
