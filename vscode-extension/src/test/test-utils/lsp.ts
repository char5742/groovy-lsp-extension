// biome-ignore lint/style/noNamespaceImport: VSCode APIを使用
// biome-ignore lint/correctness/noUndeclaredDependencies: VSCodeが提供
import * as vscode from 'vscode';
import type { LanguageClient } from 'vscode-languageclient/node';

/**
 * ドキュメントを開いてLSPの初期化を待つ
 * @param code ドキュメントのコンテンツ
 * @param lang 言語ID（デフォルト: groovy）
 * @returns 開いたドキュメント
 */
export async function openDoc(code: string, lang = 'groovy'): Promise<vscode.TextDocument> {
  const doc = await vscode.workspace.openTextDocument({
    content: code,
    language: lang,
  });

  await vscode.window.showTextDocument(doc);

  // LSPが初期化されるまで少し待つ
  await new Promise((resolve) => setTimeout(resolve, 100));

  return doc;
}

/**
 * ドキュメントを閉じる
 * @param doc 閉じるドキュメント
 */
export async function closeDoc(doc: vscode.TextDocument): Promise<void> {
  const workspaceEdit = new vscode.WorkspaceEdit();
  workspaceEdit.deleteFile(doc.uri);
  await vscode.workspace.applyEdit(workspaceEdit);
}

/**
 * 指定位置でホバー情報を取得
 * @param doc ドキュメント
 * @param position 位置
 * @returns ホバー情報
 */
export async function getHoverAt(doc: vscode.TextDocument, position: vscode.Position): Promise<vscode.Hover[]> {
  return await vscode.commands.executeCommand<vscode.Hover[]>('vscode.executeHoverProvider', doc.uri, position);
}

/**
 * 指定位置で補完候補を取得
 * @param doc ドキュメント
 * @param position 位置
 * @returns 補完候補リスト
 */
export async function getCompletionsAt(
  doc: vscode.TextDocument,
  position: vscode.Position,
): Promise<vscode.CompletionList> {
  return await vscode.commands.executeCommand<vscode.CompletionList>(
    'vscode.executeCompletionItemProvider',
    doc.uri,
    position,
  );
}

/**
 * ドキュメントの診断情報を取得
 * @param doc ドキュメント
 * @returns 診断情報
 */
export async function getDiagnostics(doc: vscode.TextDocument): Promise<vscode.Diagnostic[]> {
  // 診断が更新されるまで少し待つ
  await new Promise((resolve) => setTimeout(resolve, 500));

  return vscode.languages.getDiagnostics(doc.uri);
}

/**
 * Language Clientを取得
 * @returns Language Client
 */
export async function getLanguageClient(): Promise<LanguageClient | undefined> {
  const extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
  if (!extension) {
    throw new Error('Groovy LSP拡張機能が見つかりません');
  }

  if (!extension.isActive) {
    await extension.activate();
  }

  return extension.exports?.client;
}

/**
 * 定義へジャンプの位置を取得
 * @param doc ドキュメント
 * @param position 位置
 * @returns 定義の位置
 */
export async function getDefinitionAt(doc: vscode.TextDocument, position: vscode.Position): Promise<vscode.Location[]> {
  return await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeDefinitionProvider', doc.uri, position);
}

/**
 * 参照の検索
 * @param doc ドキュメント
 * @param position 位置
 * @returns 参照の位置リスト
 */
export async function getReferencesAt(doc: vscode.TextDocument, position: vscode.Position): Promise<vscode.Location[]> {
  return await vscode.commands.executeCommand<vscode.Location[]>('vscode.executeReferenceProvider', doc.uri, position);
}

/**
 * ワークスペースシンボルの検索
 * @param query 検索クエリ
 * @returns シンボル情報リスト
 */
export async function getWorkspaceSymbols(query: string): Promise<vscode.SymbolInformation[]> {
  return await vscode.commands.executeCommand<vscode.SymbolInformation[]>(
    'vscode.executeWorkspaceSymbolProvider',
    query,
  );
}
