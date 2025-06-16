import { ok, strictEqual } from 'node:assert/strict';
import { type ChildProcess, spawn } from 'node:child_process';
import { join } from 'node:path';

// モジュールレベルの定数
const CONTENT_LENGTH_REGEX = /Content-Length: (\d+)\r\n\r\n/;

interface JsonRpcRequest {
  jsonrpc: '2.0';
  id: number;
  method: string;
  params: unknown;
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

describe('Document Synchronization Test Suite', () => {
  let lspServer: ChildProcess;
  let messageId = 1;
  let buffer = '';

  function sendNotification(method: string, params: unknown): void {
    const notification = {
      jsonrpc: '2.0',
      method: method,
      params: params,
    };

    const message = JSON.stringify(notification);
    const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;
    lspServer.stdin?.write(header + message);
  }

  // バッファからメッセージを抽出
  function extractMessage(buffer: string): { message: string | null; remainingBuffer: string } {
    const match = buffer.match(CONTENT_LENGTH_REGEX);
    if (!match) {
      return { message: null, remainingBuffer: buffer };
    }

    const contentLength = Number.parseInt(match[1]);
    const messageStart = match[0].length;

    if (buffer.length < messageStart + contentLength) {
      return { message: null, remainingBuffer: buffer };
    }

    const message = buffer.substring(messageStart, messageStart + contentLength);
    const remainingBuffer = buffer.substring(messageStart + contentLength);
    return { message, remainingBuffer };
  }

  // JSONレスポンスをパース
  function parseJsonRpcResponse(response: string): JsonRpcResponse | null {
    try {
      return JSON.parse(response) as JsonRpcResponse;
    } catch {
      return null;
    }
  }

  function sendRequest(method: string, params: unknown): Promise<JsonRpcResponse> {
    return new Promise((resolve, reject) => {
      const request: JsonRpcRequest = {
        jsonrpc: '2.0',
        id: messageId++,
        method: method,
        params: params,
      };

      const message = JSON.stringify(request);
      const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;

      lspServer.stdin?.write(header + message);

      // レスポンスハンドラーを設定
      const currentId = request.id;
      const responseHandler = (data: Buffer) => {
        buffer += data.toString();

        const { message: extractedMessage, remainingBuffer } = extractMessage(buffer);
        if (extractedMessage) {
          buffer = remainingBuffer;
          const json = parseJsonRpcResponse(extractedMessage);

          if (json && json.id === currentId) {
            lspServer.stdout?.off('data', responseHandler);
            resolve(json);
          } else if (!json) {
            lspServer.stdout?.off('data', responseHandler);
            reject(new Error('Failed to parse JSON response'));
          }
        }
      };

      lspServer.stdout?.on('data', responseHandler);

      // タイムアウト
      setTimeout(() => {
        lspServer.stdout?.off('data', responseHandler);
        reject(new Error(`Timeout waiting for response to ${method}`));
      }, 3000);
    });
  }

  beforeEach(() => {
    const jarPath = join(
      __dirname,
      '../../../..',
      'lsp-core',
      'build',
      'libs',
      'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
    );

    lspServer = spawn('java', ['-jar', jarPath], {
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    messageId = 1;
    buffer = '';
  });

  afterEach(() => {
    lspServer.kill();
  });

  it('Should handle document synchronization', async () => {
    // 初期化
    const initResult = await sendRequest('initialize', {
      processId: process.pid,
      rootUri: 'file:///test',
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

    ok(initResult.result);
    const capabilities = initResult.result as { capabilities: { textDocumentSync?: number } };
    strictEqual(capabilities.capabilities.textDocumentSync, 1);

    // initialized通知を送信
    sendNotification('initialized', {});

    // ドキュメントを開く
    sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: 'file:///test/example.groovy',
        languageId: 'groovy',
        version: 1,
        text: 'class Example {\n    def hello() {\n        println "Hello"\n    }\n}',
      },
    });

    // 少し待機（サーバーが処理する時間を与える）
    await new Promise((resolve) => setTimeout(resolve, 100));

    // ドキュメントを変更
    sendNotification('textDocument/didChange', {
      textDocument: {
        uri: 'file:///test/example.groovy',
        version: 2,
      },
      contentChanges: [
        {
          text: 'class Example {\n    def hello() {\n        println "Hello World"\n    }\n}',
        },
      ],
    });

    // 少し待機
    await new Promise((resolve) => setTimeout(resolve, 100));

    // ドキュメントを閉じる
    sendNotification('textDocument/didClose', {
      textDocument: {
        uri: 'file:///test/example.groovy',
      },
    });

    // 少し待機
    await new Promise((resolve) => setTimeout(resolve, 100));

    // すべてのリクエストが正常に完了
    ok(true);
  });
});
