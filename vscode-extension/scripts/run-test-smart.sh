#!/bin/bash

# テスト実行と結果の解析
echo "TypeScriptテストを実行中..."

# テスト実行（出力をファイルに保存）
npm run test 2>&1 | tee test-output-temp.log &
TEST_PID=$!

# テストが完了するまで待つ（最大240秒）
TIMEOUT=240
ELAPSED=0
SUCCESS_DETECTED=false

while [ $ELAPSED -lt $TIMEOUT ]; do
    # プロセスが終了したかチェック
    if ! kill -0 $TEST_PID 2>/dev/null; then
        wait $TEST_PID
        EXIT_CODE=$?
        SUCCESS_DETECTED=true
        break
    fi
    
    # 出力ファイルからテスト完了を検出
    if [ -f test-output-temp.log ]; then
        # すべてのテストが成功したパターンを検出
        if grep -q "passing" test-output-temp.log && grep -q "BUILD SUCCESSFUL" test-output-temp.log 2>/dev/null; then
            # テストが成功した可能性が高い
            sleep 5  # 少し待つ
            
            # まだプロセスが残っていたら終了
            if kill -0 $TEST_PID 2>/dev/null; then
                echo "テストは成功しましたが、プロセスが残っています。強制終了します。"
                kill -TERM $TEST_PID 2>/dev/null || true
                sleep 2
                kill -KILL $TEST_PID 2>/dev/null || true
                SUCCESS_DETECTED=true
                EXIT_CODE=0
                break
            fi
        fi
    fi
    
    sleep 1
    ELAPSED=$((ELAPSED + 1))
done

# タイムアウトした場合
if [ "$SUCCESS_DETECTED" = "false" ]; then
    echo "テストがタイムアウトしました（${TIMEOUT}秒）"
    kill -TERM $TEST_PID 2>/dev/null || true
    sleep 2
    kill -KILL $TEST_PID 2>/dev/null || true
    EXIT_CODE=1
fi

# クリーンアップ
rm -f test-output-temp.log
pkill -f "vscode-test" 2>/dev/null || true
pkill -f "xvfb-run" 2>/dev/null || true

exit $EXIT_CODE