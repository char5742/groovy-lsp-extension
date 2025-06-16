import { ok } from 'node:assert/strict';
import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { type DocumentSymbol, SymbolKind, Uri, commands, window, workspace } from 'vscode';

describe('DocumentSymbol Test Suite', () => {
  let tempDir: string;

  beforeEach(() => {
    // 一時ディレクトリを作成
    tempDir = join(tmpdir(), `groovy-lsp-test-${Date.now()}`);
    mkdirSync(tempDir, { recursive: true });
  });

  afterEach(async () => {
    // 開いているエディタを閉じる
    await commands.executeCommand('workbench.action.closeAllEditors');

    // 一時ディレクトリを削除
    try {
      rmSync(tempDir, { recursive: true, force: true });
    } catch {
      // エラーは無視
    }
  });

  it('Should provide document symbols for classes and methods @core @document-symbol', async () => {
    // Groovyファイルを作成
    const content = `
class Calculator {
    int add(int a, int b) {
        return a + b
    }
    
    int subtract(int a, int b) {
        return a - b
    }
}`;
    const filePath = join(tempDir, 'Calculator.groovy');
    writeFileSync(filePath, content);

    // ファイルを開く
    const document = await workspace.openTextDocument(Uri.file(filePath));
    await window.showTextDocument(document);

    // LSPサーバーが処理するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));

    // DocumentSymbolを取得
    const symbols = await commands.executeCommand<DocumentSymbol[]>(
      'vscode.executeDocumentSymbolProvider',
      document.uri,
    );

    // 検証
    ok(symbols, 'シンボルが取得できること');
    ok(symbols.length === 1, '1つのクラスシンボルが取得できること');

    const classSymbol = symbols[0];
    ok(classSymbol.name === 'Calculator', 'クラス名が正しいこと');
    ok(classSymbol.kind === SymbolKind.Class, 'クラスとして認識されること');
    ok(classSymbol.children.length === 2, '2つのメソッドが含まれること');

    const methods = classSymbol.children.map((c) => c.name);
    ok(methods.includes('add'), 'addメソッドが存在すること');
    ok(methods.includes('subtract'), 'subtractメソッドが存在すること');
  });

  it('Should provide document symbols for fields and properties @core @document-symbol', async () => {
    const content = `
class Person {
    private String name
    int age
    String address
}`;
    const filePath = join(tempDir, 'Person.groovy');
    writeFileSync(filePath, content);

    const document = await workspace.openTextDocument(Uri.file(filePath));
    await window.showTextDocument(document);

    await new Promise((resolve) => setTimeout(resolve, 2000));

    const symbols = await commands.executeCommand<DocumentSymbol[]>(
      'vscode.executeDocumentSymbolProvider',
      document.uri,
    );

    ok(symbols, 'シンボルが取得できること');
    ok(symbols.length === 1, '1つのクラスシンボルが取得できること');

    const classSymbol = symbols[0];
    ok(classSymbol.name === 'Person', 'クラス名が正しいこと');
    ok(classSymbol.children.length >= 3, '少なくとも3つのフィールドが含まれること');

    const fieldNames = classSymbol.children.map((c) => c.name);
    ok(fieldNames.includes('name'), 'nameフィールドが存在すること');
    ok(fieldNames.includes('age'), 'ageフィールドが存在すること');
    ok(fieldNames.includes('address'), 'addressフィールドが存在すること');
  });

  it('Should provide document symbols for interfaces @core @document-symbol', async () => {
    const content = `
interface Runnable {
    void run()
}

class Thread implements Runnable {
    void run() {
        println "Running"
    }
}`;
    const filePath = join(tempDir, 'Thread.groovy');
    writeFileSync(filePath, content);

    const document = await workspace.openTextDocument(Uri.file(filePath));
    await window.showTextDocument(document);

    await new Promise((resolve) => setTimeout(resolve, 2000));

    const symbols = await commands.executeCommand<DocumentSymbol[]>(
      'vscode.executeDocumentSymbolProvider',
      document.uri,
    );

    ok(symbols, 'シンボルが取得できること');
    ok(symbols.length === 2, 'インターフェースとクラスの2つのシンボルが取得できること');

    const symbolNames = symbols.map((s) => s.name);
    ok(symbolNames.includes('Runnable'), 'Runnableインターフェースが存在すること');
    ok(symbolNames.includes('Thread'), 'Threadクラスが存在すること');

    const interfaceSymbol = symbols.find((s) => s.name === 'Runnable');
    ok(interfaceSymbol?.kind === SymbolKind.Interface, 'インターフェースとして認識されること');

    const classSymbol = symbols.find((s) => s.name === 'Thread');
    ok(classSymbol?.kind === SymbolKind.Class, 'クラスとして認識されること');
  });

  it('Should handle empty files gracefully @core @document-symbol', async () => {
    const content = '';
    const filePath = join(tempDir, 'Empty.groovy');
    writeFileSync(filePath, content);

    const document = await workspace.openTextDocument(Uri.file(filePath));
    await window.showTextDocument(document);

    // 空ファイルの場合、LSPサーバーが処理するまで少し長めに待機
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const symbols = await commands.executeCommand<DocumentSymbol[]>(
      'vscode.executeDocumentSymbolProvider',
      document.uri,
    );

    // VSCodeが空ファイルの場合にundefinedを返す可能性があるため、
    // undefinedまたは空配列の両方を許容する
    ok(symbols !== null, 'nullではないこと');
    if (symbols !== undefined) {
      ok(Array.isArray(symbols), '配列であること');
      ok(symbols.length === 0, '空の配列が返されること');
    }
  });
});
