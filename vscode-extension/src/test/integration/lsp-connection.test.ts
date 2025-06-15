// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as assert from 'node:assert/strict';
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import { spawn } from 'node:child_process';
// biome-ignore lint/style/noNamespaceImport: テストで必要
// biome-ignore lint/correctness/noNodejsModules: テストで必要
import * as path from 'node:path';

// モジュールレベルの定数
const CONTENT_LENGTH_REGEX = /Content-Length: (\d+)\r\n\r\n/;

interface JsonRpcResponse {
  jsonrpc: '2.0';
  id: number;
  result?: {
    capabilities: Record<string, unknown>;
  };
  error?: {
    code: number;
    message: string;
    data?: unknown;
  };
}

describe('LSP Connection Test Suite', () => {
  // biome-ignore lint/style/noDoneCallback: LSPサーバーの非同期レスポンスをテストするため必要
  it('LSP server should respond to initialize request', (done) => {
    const jarPath = path.join(
      __dirname,
      '../../../..',
      'lsp-core',
      'build',
      'libs',
      'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
    );

    // LSPサーバーを起動
    const lspServer = spawn('java', ['-jar', jarPath], {
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    // initializeリクエストを送信
    const initializeRequest = {
      jsonrpc: '2.0',
      id: 1,
      method: 'initialize',
      params: {
        processId: process.pid,
        rootUri: null,
        capabilities: {},
        workspaceFolders: null,
      },
    };

    const message = JSON.stringify(initializeRequest);
    const header = `Content-Length: ${Buffer.byteLength(message)}\r\n\r\n`;

    lspServer.stdin.write(header + message);

    // サーバー出力を処理
    let buffer = '';
    lspServer.stdout.on('data', (data) => {
      buffer += data.toString();

      // レスポンスのパースを試行
      const match = buffer.match(CONTENT_LENGTH_REGEX);
      if (match) {
        const contentLength = Number.parseInt(match[1]);
        const messageStart = match[0].length;

        if (buffer.length >= messageStart + contentLength) {
          const response = buffer.substring(messageStart, messageStart + contentLength);
          try {
            const json = JSON.parse(response) as JsonRpcResponse;
            assert.strictEqual(json.id, 1);
            assert.ok(json.result);
            assert.ok(json.result.capabilities);
            lspServer.kill();
            done();
          } catch (e) {
            lspServer.kill();
            done(e);
          }
        }
      }
    });

    // 5秒後にタイムアウト
    setTimeout(() => {
      lspServer.kill();
      done(new Error('Timeout: No response from LSP server'));
    }, 5000);
  });
});
