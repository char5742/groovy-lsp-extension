#!/usr/bin/env python3
"""
Interactive LSP Server Test
Tests each message individually with proper response parsing
"""

import json
import subprocess
import time
import os
import sys

class LSPTester:
    def __init__(self, server_path):
        self.server_path = server_path
        self.process = None
        
    def start_server(self):
        """Start the LSP server process"""
        print("Starting LSP server...")
        self.process = subprocess.Popen(
            [self.server_path],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=0  # Unbuffered
        )
        time.sleep(0.5)  # Give server time to start
        
    def send_message(self, method, params, msg_id=None):
        """Send a JSON-RPC message and return the response"""
        message = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params
        }
        if msg_id is not None:
            message["id"] = msg_id
            
        json_str = json.dumps(message)
        content_length = len(json_str.encode('utf-8'))
        
        # Send with Content-Length header
        full_message = f"Content-Length: {content_length}\r\n\r\n{json_str}"
        print(f"\n>>> Sending: {method}")
        print(f"    {json.dumps(params, indent=4) if params else 'null'}")
        
        self.process.stdin.write(full_message.encode('utf-8'))
        self.process.stdin.flush()
        
        # Read response if this was a request (has ID)
        if msg_id is not None:
            return self.read_response()
        else:
            time.sleep(0.1)  # Brief pause for notifications
            return None
            
    def read_response(self):
        """Read and parse a JSON-RPC response"""
        # Read headers
        headers = {}
        while True:
            line = self.process.stdout.readline().decode('utf-8')
            if line == '\r\n' or line == '\n':
                break
            if ':' in line:
                key, value = line.strip().split(':', 1)
                headers[key.strip()] = value.strip()
                
        # Read content
        if 'Content-Length' in headers:
            content_length = int(headers['Content-Length'])
            content = self.process.stdout.read(content_length).decode('utf-8')
            response = json.loads(content)
            print(f"<<< Response:")
            print(f"    {json.dumps(response, indent=4)}")
            return response
            
        return None
        
    def test_lifecycle(self):
        """Test the full LSP lifecycle"""
        print("\n=== Testing LSP Server Lifecycle ===\n")
        
        self.start_server()
        
        # 1. Initialize
        print("Step 1: Initialize")
        response = self.send_message("initialize", {
            "processId": os.getpid(),
            "rootUri": "file:///tmp/test-workspace",
            "capabilities": {
                "textDocument": {
                    "synchronization": {
                        "dynamicRegistration": True,
                        "willSave": True,
                        "willSaveWaitUntil": True,
                        "didSave": True
                    }
                }
            },
            "trace": "verbose"
        }, msg_id=1)
        
        if response and 'result' in response:
            print("✓ Initialize successful")
            caps = response['result'].get('capabilities', {})
            if caps:
                print(f"  Server capabilities: {list(caps.keys())}")
            else:
                print("  Server capabilities: (none advertised)")
        
        # 2. Initialized notification
        print("\nStep 2: Initialized notification")
        self.send_message("initialized", {})
        print("✓ Notification sent")
        
        # 3. Test document synchronization (if supported)
        print("\nStep 3: Test document operations")
        self.send_message("textDocument/didOpen", {
            "textDocument": {
                "uri": "file:///tmp/test.groovy",
                "languageId": "groovy",
                "version": 1,
                "text": "println 'Hello, World!'"
            }
        })
        print("✓ Document opened")
        
        # 4. Shutdown
        print("\nStep 4: Shutdown")
        response = self.send_message("shutdown", None, msg_id=2)
        if response and 'result' in response:
            print("✓ Shutdown successful")
            
        # 5. Exit
        print("\nStep 5: Exit")
        self.send_message("exit", None)
        print("✓ Exit sent")
        
        # Wait for process to terminate
        try:
            self.process.wait(timeout=2)
            print(f"\n✓ Server exited with code: {self.process.returncode}")
        except subprocess.TimeoutExpired:
            print("\n✗ Server did not exit within timeout")
            self.process.terminate()
            
        # Check for any errors
        stderr = self.process.stderr.read().decode('utf-8')
        if stderr:
            print("\n=== Server logs ===")
            print(stderr[:500])  # First 500 chars

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    lsp_core_dir = os.path.join(script_dir, '../../..')
    launch_script = os.path.join(lsp_core_dir, 'build/install/lsp-core/bin/lsp-core')
    
    if not os.path.exists(launch_script):
        print(f"Error: Launch script not found at {launch_script}")
        print("Please run './gradlew installDist' first")
        sys.exit(1)
        
    tester = LSPTester(launch_script)
    tester.test_lifecycle()

if __name__ == "__main__":
    main()