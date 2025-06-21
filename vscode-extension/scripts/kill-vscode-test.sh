#!/bin/bash

# VSCode test processを検索して強制終了
pkill -f "vscode-test" || true
pkill -f "xvfb-run" || true

exit 0