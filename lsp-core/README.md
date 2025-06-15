# Groovy Language Server

Groovy用の最小限のLanguage Server Protocol (LSP)実装。

## 必要要件

- Java 23以上

## ビルド

```bash
./gradlew build
```

## 実行

標準I/OモードでLSPサーバーを起動するには:

```bash
./gradlew run
```

サーバーは標準入出力ストリームを介してJSON-RPCで通信します。

## JSON-RPC通信のテスト

標準入力にJSON-RPCメッセージを送信してサーバーをテストできます。初期化リクエストの例:

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

サーバーは標準出力に機能情報を返します。