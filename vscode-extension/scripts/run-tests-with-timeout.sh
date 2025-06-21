#!/bin/bash

# タイムアウト時間（秒）
TIMEOUT=240

# テストコマンドをバックグラウンドで実行
npm run test &
TEST_PID=$!

# テストプロセスを監視
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if ! kill -0 $TEST_PID 2>/dev/null; then
        # プロセスが終了した
        wait $TEST_PID
        EXIT_CODE=$?
        
        # クリーンアップ
        ./scripts/kill-vscode-test.sh
        
        exit $EXIT_CODE
    fi
    
    # 1秒待つ
    sleep 1
    ELAPSED=$((ELAPSED + 1))
done

# タイムアウトした場合
echo "テストがタイムアウトしました（${TIMEOUT}秒）"
kill -TERM $TEST_PID 2>/dev/null || true
sleep 2
kill -KILL $TEST_PID 2>/dev/null || true

# クリーンアップ
./scripts/kill-vscode-test.sh

exit 1