# Groovy Language Server

Minimal Language Server Protocol (LSP) implementation for Groovy.

## Requirements

- Java 23 or higher

## Build

```bash
./gradlew build
```

## Run

To start the LSP server in standard I/O mode:

```bash
./gradlew run
```

The server communicates via JSON-RPC over standard input/output streams.

## Test JSON-RPC Communication

You can test the server by sending JSON-RPC messages to stdin. Example initialization request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "processId": null,
    "capabilities": {},
    "rootUri": "file:///path/to/workspace"
  }
}
```

The server will respond with its capabilities on stdout.