# テストシナリオ一覧

## 概要

- **総テスト数**: 5
- **単体テスト**: 3
- **統合テスト**: 2
- **E2Eテスト**: 0

## 単体テスト

### Extension Test Suite
*ファイル: src/test/suite/extension.test.ts*

- Extension should be present @core @initialization [core, initialization]
- Extension should activate @core @activation [core, activation]
- Should register Groovy language @core @language-registration [core, language]

## 統合テスト

### Document Synchronization Test Suite
*ファイル: src/test/integration/document-sync.test.ts*

- Should handle document synchronization

### LSP Connection Test Suite
*ファイル: src/test/integration/lsp-connection.test.ts*

- LSP server should respond to initialize request

