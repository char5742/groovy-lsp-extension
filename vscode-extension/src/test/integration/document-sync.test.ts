import * as assert from 'node:assert';
import { type ChildProcess, spawn } from 'node:child_process';
import * as path from 'node:path';

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

suite('Document Synchronization Test Suite', () => {
  let lspServer: ChildProcess;
  let messageId = 1;
  let buffer = '';

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

        const match = buffer.match(/Content-Length: (\d+)\r\n\r\n/);
        if (match) {
          const contentLength = Number.parseInt(match[1]);
          const messageStart = match[0].length;

          if (buffer.length >= messageStart + contentLength) {
            const response = buffer.substring(messageStart, messageStart + contentLength);
            buffer = buffer.substring(messageStart + contentLength);

            try {
              const json = JSON.parse(response) as JsonRpcResponse;
              if (json.id === currentId) {
                lspServer.stdout?.off('data', responseHandler);
                resolve(json);
              }
            } catch (e) {
              reject(e);
            }
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

  setup(() => {
    const jarPath = path.join(
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

  teardown(() => {
    lspServer.kill();
  });

  // TODO: LSPサーバーのJARファイルがビルドされたら有効化する
  test.skip('Should handle document synchronization', async () => {
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

    assert.ok(initResult.result);
    const capabilities = initResult.result as { capabilities: { textDocumentSync?: number } };
    assert.strictEqual(capabilities.capabilities.textDocumentSync, 1);

    // initialized通知を送信
    await sendRequest('initialized', {});

    // ドキュメントを開く
    await sendRequest('textDocument/didOpen', {
      textDocument: {
        uri: 'file:///test/example.groovy',
        languageId: 'groovy',
        version: 1,
        text: 'class Example {\n    def hello() {\n        println "Hello"\n    }\n}',
      },
    });

    // ドキュメントを変更
    await sendRequest('textDocument/didChange', {
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

    // ドキュメントを閉じる
    await sendRequest('textDocument/didClose', {
      textDocument: {
        uri: 'file:///test/example.groovy',
      },
    });

    // すべてのリクエストが正常に完了
    assert.ok(true);
  });
});
