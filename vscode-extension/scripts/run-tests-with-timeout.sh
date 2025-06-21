#!/bin/bash

# タイムアウト時間（秒）
TIMEOUT=240

# テストコマンドを実行
timeout --preserve-status $TIMEOUT npm run test

# タイムアウトした場合のエラーコード
if [ $? -eq 124 ]; then
    echo "テストがタイムアウトしました（${TIMEOUT}秒）"
    exit 1
fi

exit 0