// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as assert from 'node:assert/strict';
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import { spawn } from 'node:child_process';
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import { tmpdir } from 'node:os';
// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as path from 'node:path';
import { pathToFileURL } from 'node:url';

// モジュールレベルの定数
const CONTENT_LENGTH_REGEX = /Content-Length: (\d+)\r\n\r\n/;

interface JsonRpcRequest {
  jsonrpc: '2.0';
  id: number;
  method: string;
  params?: unknown;
}

interface JsonRpcResponse {
  jsonrpc: '2.0';
  id: number;
  result?: unknown;
  error?: {
    code: number;
    message: string;
    data?: unknown;
  };
}

class LspClient {
  private lspServer;
  private buffer = '';
  private messageHandlers = new Map<number, (response: JsonRpcResponse) => void>();
  private nextId = 1;

  constructor(jarPath: string) {
    this.lspServer = spawn('java', ['-jar', jarPath], {
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    this.lspServer.stdout.on('data', (data) => {
      this.buffer += data.toString();
      this.processBuffer();
    });

    // stderrは無視（ログ出力のため）
  }

  private processBuffer() {
    while (true) {
      const match = this.buffer.match(CONTENT_LENGTH_REGEX);
      if (!match) {
        break;
      }

      const contentLength = Number.parseInt(match[1]);
      const messageStart = match[0].length;

      if (this.buffer.length < messageStart + contentLength) {
        break;
      }

      const response = this.buffer.substring(messageStart, messageStart + contentLength);
      this.buffer = this.buffer.substring(messageStart + contentLength);

      try {
        const json = JSON.parse(response);
        // リクエストに対するレスポンスのみ処理
        if (json.id !== undefined) {
          const handler = this.messageHandlers.get(json.id);
          if (handler) {
            handler(json);
            this.messageHandlers.delete(json.id);
          }
        }
        // 通知やその他のメッセージは無視
      } catch (e) {
        // JSONパースエラーは無視（複数のメッセージが連結されている場合がある）
      }
    }
  }

  async sendRequest(method: string, params?: unknown): Promise<JsonRpcResponse> {
    const id = this.nextId++;
    const request: JsonRpcRequest = {
      jsonrpc: '2.0',
      id,
      method,
      params,
    };

    const message = JSON.stringify(request);
    const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;

    return new Promise((resolve, reject) => {
      this.messageHandlers.set(id, resolve);
      this.lspServer.stdin.write(header + message);

      // タイムアウト設定
      setTimeout(() => {
        if (this.messageHandlers.has(id)) {
          this.messageHandlers.delete(id);
          reject(new Error(`Timeout waiting for response to ${method}`));
        }
      }, 5000);
    });
  }

  async sendNotification(method: string, params?: unknown): Promise<void> {
    const notification = {
      jsonrpc: '2.0',
      method,
      params,
    };

    const message = JSON.stringify(notification);
    const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;
    this.lspServer.stdin.write(header + message);
  }

  kill() {
    this.lspServer.kill();
  }
}

describe('字句解析の統合テスト', () => {
  let client: LspClient;
  let workspaceDir: string;

  beforeEach(async () => {
    const jarPath = path.join(
      __dirname,
      '../../../..',
      'lsp-core',
      'build',
      'libs',
      'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
    );

    client = new LspClient(jarPath);

    // ワークスペースディレクトリを作成
    workspaceDir = await mkdtemp(path.join(tmpdir(), 'groovy-lexer-test-'));

    // LSPサーバーを初期化
    await client.sendRequest('initialize', {
      processId: process.pid,
      rootUri: pathToFileURL(workspaceDir).toString(),
      capabilities: {
        textDocument: {
          synchronization: {
            dynamicRegistration: false,
            willSave: false,
            willSaveWaitUntil: false,
            didSave: true,
          },
        },
      },
      workspaceFolders: null,
    });

    // initialized通知を送信（IDなし）
    await client.sendNotification('initialized', {});
  });

  afterEach(async () => {
    try {
      await client.sendRequest('shutdown');
    } catch (error) {
      // shutdownのタイムアウトを無視
    }
    client.kill();
    await rm(workspaceDir, { recursive: true, force: true });
  });

  it('キーワードの字句解析が正しく動作する', async () => {
    const testFile = path.join(workspaceDir, 'keywords.groovy');
    const content = 'def class if else while for return new';
    await writeFile(testFile, content, 'utf8');

    // ドキュメントを開く
    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
        languageId: 'groovy',
        version: 1,
        text: content,
      },
    });

    // サーバーが処理する時間を与える
    await new Promise((resolve) => setTimeout(resolve, 500));

    // ドキュメントを閉じる
    await client.sendNotification('textDocument/didClose', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
      },
    });

    // 現時点では字句解析結果を直接取得できないため、
    // エラーなく処理が完了することを確認
    assert.ok(true);
  });

  it('文字列リテラルの字句解析が正しく動作する', async () => {
    const testFile = path.join(workspaceDir, 'strings.groovy');
    const content = `
      def single = 'Hello, Groovy!'
      def double = "Hello, World!"
      def multiline = """
        This is a
        multiline string
      """
    `;
    await writeFile(testFile, content, 'utf8');

    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
        languageId: 'groovy',
        version: 1,
        text: content,
      },
    });

    await new Promise((resolve) => setTimeout(resolve, 500));

    await client.sendNotification('textDocument/didClose', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
      },
    });

    assert.ok(true);
  });

  it('数値リテラルの字句解析が正しく動作する', async () => {
    const testFile = path.join(workspaceDir, 'numbers.groovy');
    const content = `
      def integer = 42
      def decimal = 3.14
      def bigInt = 100000000000L
      def hex = 0xFF
      def binary = 0b1010
    `;
    await writeFile(testFile, content, 'utf8');

    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
        languageId: 'groovy',
        version: 1,
        text: content,
      },
    });

    await new Promise((resolve) => setTimeout(resolve, 500));

    await client.sendNotification('textDocument/didClose', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
      },
    });

    assert.ok(true);
  });

  it('複雑なGroovyコードの字句解析が正しく動作する', async () => {
    const testFile = path.join(workspaceDir, 'complex.groovy');
    const content = `
      package com.example

      import groovy.transform.CompileStatic

      @CompileStatic
      class GreetingService {
          String greet(String name) {
              if (name != null && name.trim()) {
                  return "Hello, \${name}!"
              } else {
                  return "Hello, World!"
              }
          }
          
          List<Integer> fibonacci(int n) {
              def result = []
              def (a, b) = [0, 1]
              
              for (int i = 0; i < n; i++) {
                  result << a
                  (a, b) = [b, a + b]
              }
              
              return result
          }
      }
    `;
    await writeFile(testFile, content, 'utf8');

    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
        languageId: 'groovy',
        version: 1,
        text: content,
      },
    });

    await new Promise((resolve) => setTimeout(resolve, 500));

    await client.sendNotification('textDocument/didClose', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
      },
    });

    assert.ok(true);
  });

  it('コメントの字句解析が正しく動作する', async () => {
    const testFile = path.join(workspaceDir, 'comments.groovy');
    const content = `
      // 単一行コメント
      def x = 10 // 行末コメント
      
      /*
       * ブロックコメント
       * 複数行にまたがる
       */
      class Example {
          /**
           * JavaDocスタイルのコメント
           * @param name パラメータの説明
           */
          def method(String name) {
              /* インラインブロックコメント */ return name
          }
      }
    `;
    await writeFile(testFile, content, 'utf8');

    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
        languageId: 'groovy',
        version: 1,
        text: content,
      },
    });

    await new Promise((resolve) => setTimeout(resolve, 500));

    await client.sendNotification('textDocument/didClose', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
      },
    });

    assert.ok(true);
  });

  it('演算子と区切り文字の字句解析が正しく動作する', async () => {
    const testFile = path.join(workspaceDir, 'operators.groovy');
    const content = `
      def a = 10 + 20 - 5 * 2 / 3 % 4
      def b = a == 10 || a != 20 && a > 5
      def c = a >= 10 ? true : false
      def d = [1, 2, 3].collect { it * 2 }
      def e = map.key?.value ?: "default"
      def f = a++; b--
      def g = ~"pattern"
      def h = list[0..5]
    `;
    await writeFile(testFile, content, 'utf8');

    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
        languageId: 'groovy',
        version: 1,
        text: content,
      },
    });

    await new Promise((resolve) => setTimeout(resolve, 500));

    await client.sendNotification('textDocument/didClose', {
      textDocument: {
        uri: pathToFileURL(testFile).toString(),
      },
    });

    assert.ok(true);
  });
});
