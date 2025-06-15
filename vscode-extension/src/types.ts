import type { LanguageClient } from 'vscode-languageclient/node';

/**
 * 拡張機能のアクティベーション結果
 */
export interface ExtensionApi {
  /** Language Server Protocol クライアント */
  client: LanguageClient;
}
