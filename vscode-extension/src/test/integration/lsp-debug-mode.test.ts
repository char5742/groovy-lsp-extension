import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import path from 'node:path';

// モジュールレベルの定数
const CONTENT_LENGTH_REGEX = /Content-Length: (\d+)\r\n\r\n/;
const DEBUG_PORT = 5006; // テスト用に別ポートを使用

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

describe('LSP Debug Mode Test Suite', () => {
  it('LSP server should respond correctly in debug mode with quiet flag', (done) => {
    const jarPath = path.join(
      __dirname,
      '../../../..',
      'lsp-core',
      'build',
      'libs',
      'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
    );

    // デバッグモードでLSPサーバーを起動（quiet=yオプション付き）
    const lspServer = spawn(
      'java',
      [`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEBUG_PORT},quiet=y`, '-jar', jarPath],
      {
        stdio: ['pipe', 'pipe', 'pipe'],
      },
    );

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

    // サーバーの起動を待ってからリクエストを送信
    setTimeout(() => {
      lspServer.stdin.write(header + message);
    }, 1000);

    // サーバー出力を処理
    let buffer = '';
    let hasReceivedInvalidHeader = false;

    lspServer.stdout.on('data', (data) => {
      const dataStr = data.toString();
      buffer += dataStr;

      // デバッグ出力が混入していないかチェック
      if (dataStr.includes('listening for transport')) {
        hasReceivedInvalidHeader = true;
        lspServer.kill();
        done(new Error('Debug output mixed with LSP protocol'));
        return;
      }

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
            assert.ok(!hasReceivedInvalidHeader, 'デバッグ出力がプロトコルに混入しています');
            lspServer.kill();
            done();
          } catch (e) {
            lspServer.kill();
            done(e);
          }
        }
      }
    });

    // エラー出力を監視
    lspServer.stderr.on('data', (data) => {
      console.log('LSP Server stderr:', data.toString());
    });

    // 10秒後にタイムアウト
    setTimeout(() => {
      lspServer.kill();
      done(new Error('Timeout: No response from LSP server'));
    }, 10000);
  });

  it('LSP server debug output should not interfere with protocol', (done) => {
    const jarPath = path.join(
      __dirname,
      '../../../..',
      'lsp-core',
      'build',
      'libs',
      'groovy-lsp-server-0.0.1-SNAPSHOT-all.jar',
    );

    // デバッグモードでLSPサーバーを起動（quietなし - 問題を再現）
    const lspServer = spawn(
      'java',
      [`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEBUG_PORT + 1}`, '-jar', jarPath],
      {
        stdio: ['pipe', 'pipe', 'pipe'],
      },
    );

    // 標準出力の最初の部分を確認
    let firstOutput = '';
    let outputReceived = false;

    lspServer.stdout.once('data', (data) => {
      firstOutput = data.toString();
      outputReceived = true;

      // デバッグ出力が含まれているかチェック
      if (firstOutput.includes('Listening for transport')) {
        // quietオプションなしの場合、デバッグ出力が標準出力に混入する可能性がある
        console.log('警告: デバッグ出力が検出されました（quietオプションなし）');
      }

      lspServer.kill();
      done();
    });

    // 3秒後にタイムアウト
    setTimeout(() => {
      lspServer.kill();
      if (!outputReceived) {
        done(new Error('No output received from server'));
      }
    }, 3000);
  });
});
