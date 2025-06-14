#!/bin/bash
# Test LSP server standard I/O communication

echo "=== Testing Groovy LSP Server Standard I/O ==="

# Create temporary test input
TEMP_INPUT=$(mktemp)
cat > "$TEMP_INPUT" << 'EOF'
Content-Length: 205

{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":null,"rootUri":"file:///tmp/test","capabilities":{},"trace":"verbose","workspaceFolders":[{"uri":"file:///tmp/test","name":"test"}]}}
EOF

# Run from lsp-core directory
cd "$(dirname "$0")/../../.."

echo "Starting LSP server..."
timeout 5s ./gradlew -q run < "$TEMP_INPUT" 2>&1

# Cleanup
rm -f "$TEMP_INPUT"