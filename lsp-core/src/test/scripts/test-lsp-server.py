#!/usr/bin/env python3
"""
Groovy LSP Server Standard I/O Test
Tests JSON-RPC communication with proper message formatting
"""

import json
import subprocess
import time
import os

def create_json_rpc_message(method, params, id=None):
    """Create a properly formatted JSON-RPC message with Content-Length header"""
    message = {
        "jsonrpc": "2.0",
        "method": method,
        "params": params
    }
    if id is not None:
        message["id"] = id
    
    json_str = json.dumps(message)
    content_length = len(json_str.encode('utf-8'))
    
    return f"Content-Length: {content_length}\r\n\r\n{json_str}"

def test_lsp_server():
    """Test the LSP server with proper JSON-RPC messages"""
    print("=== Groovy LSP Server Standard I/O Test ===\n")
    
    # Find the launch script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    lsp_core_dir = os.path.join(script_dir, '../../..')
    launch_script = os.path.join(lsp_core_dir, 'build/install/lsp-core/bin/lsp-core')
    
    if not os.path.exists(launch_script):
        print(f"Error: Launch script not found at {launch_script}")
        print("Please run './gradlew installDist' first")
        return
    
    # Prepare test messages
    messages = []
    
    # 1. Initialize request
    messages.append(create_json_rpc_message(
        "initialize",
        {
            "processId": None,
            "rootUri": "file:///tmp/test-workspace",
            "capabilities": {},
            "trace": "verbose"
        },
        id=1
    ))
    
    # 2. Initialized notification
    messages.append(create_json_rpc_message(
        "initialized",
        {}
    ))
    
    # 3. Shutdown request
    messages.append(create_json_rpc_message(
        "shutdown",
        None,
        id=2
    ))
    
    # 4. Exit notification
    messages.append(create_json_rpc_message(
        "exit",
        None
    ))
    
    # Combine all messages
    input_data = "".join(messages).encode('utf-8')
    
    print(f"Starting LSP server from: {launch_script}")
    print(f"Sending {len(messages)} messages...\n")
    
    # Start the LSP server
    process = subprocess.Popen(
        [launch_script],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    
    try:
        # Send all messages and get output
        stdout, stderr = process.communicate(input=input_data, timeout=5)
        
        # Parse responses from stdout
        stdout_str = stdout.decode('utf-8')
        stderr_str = stderr.decode('utf-8')
        
        print("=== STDOUT (JSON-RPC Responses) ===")
        if stdout_str:
            # Try to parse JSON-RPC responses
            lines = stdout_str.split('\r\n')
            i = 0
            while i < len(lines):
                if lines[i].startswith('Content-Length:'):
                    # Found a response
                    content_length = int(lines[i].split(':')[1].strip())
                    # Skip empty line
                    i += 2
                    if i < len(lines):
                        json_content = lines[i][:content_length]
                        try:
                            response = json.loads(json_content)
                            print(json.dumps(response, indent=2))
                        except json.JSONDecodeError:
                            print(f"Invalid JSON: {json_content}")
                i += 1
        else:
            print("No response received")
        
        print("\n=== STDERR (Logs) ===")
        if stderr_str:
            # Show first few log lines
            log_lines = stderr_str.split('\n')[:10]
            for line in log_lines:
                if line.strip():
                    print(line)
            if len(stderr_str.split('\n')) > 10:
                print("... (truncated)")
        
        print(f"\nServer exit code: {process.returncode}")
        
    except subprocess.TimeoutExpired:
        print("Timeout: Server did not respond within 5 seconds")
        process.terminate()
    except Exception as e:
        print(f"Error: {e}")
        process.terminate()

if __name__ == "__main__":
    test_lsp_server()