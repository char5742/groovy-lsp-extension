#!/bin/bash
# Build script for Groovy LSP Extension

echo "Building Groovy LSP Extension..."

# Build LSP core
echo "Building LSP core..."
./gradlew :groovy-lsp:build

# TODO: Build VSCode extension when implemented
# echo "Building VSCode extension..."
# cd vscode-extension && npm run build

echo "Build complete!"