import { unlink, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  type CompletionList,
  type Diagnostic,
  type Hover,
  type Location,
  type Position,
  type SymbolInformation,
  type TextDocument,
  commands,
  extensions,
  languages,
  window,
  workspace,
} from 'vscode';
import type { LanguageClient } from 'vscode-languageclient/node';

/**
 * ドキュメントを開いてLSPの初期化を待つ
 * @param code ドキュメントのコンテンツ
 * @param lang 言語ID（デフォルト: groovy）
 * @returns 開いたドキュメント
 */
export async function openDoc(code: string, lang = 'groovy'): Promise<TextDocument> {
  // 一時ファイルを作成
  const tempDir = tmpdir();
  const tempFile = join(tempDir, `test-${Date.now()}.${lang}`);
  await writeFile(tempFile, code, 'utf8');

  // ファイルを開く
  const doc = await workspace.openTextDocument(tempFile);
  await window.showTextDocument(doc);

  // LSPが初期化されるまで少し待つ
  await new Promise((resolve) => setTimeout(resolve, 500));

  return doc;
}

/**
 * ドキュメントを閉じる
 * @param doc 閉じるドキュメント
 */
export async function closeDoc(doc: TextDocument): Promise<void> {
  // エディタを閉じる
  await commands.executeCommand('workbench.action.closeActiveEditor');

  // ファイルが存在する場合は削除
  if (doc.uri.scheme === 'file') {
    try {
      await unlink(doc.uri.fsPath);
    } catch (_error) {
      // ファイルが既に削除されている場合は無視
    }
  }
}

/**
 * 指定位置でホバー情報を取得
 * @param doc ドキュメント
 * @param position 位置
 * @returns ホバー情報
 */
export async function getHoverAt(doc: TextDocument, position: Position): Promise<Hover[]> {
  return await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', doc.uri, position);
}

/**
 * 指定位置で補完候補を取得
 * @param doc ドキュメント
 * @param position 位置
 * @returns 補完候補リスト
 */
export async function getCompletionsAt(doc: TextDocument, position: Position): Promise<CompletionList> {
  return await commands.executeCommand<CompletionList>('vscode.executeCompletionItemProvider', doc.uri, position);
}

/**
 * ドキュメントの診断情報を取得
 * @param doc ドキュメント
 * @returns 診断情報
 */
export async function getDiagnostics(doc: TextDocument): Promise<Diagnostic[]> {
  // 診断が更新されるまで少し待つ
  await new Promise((resolve) => setTimeout(resolve, 500));

  return languages.getDiagnostics(doc.uri);
}

/**
 * Language Clientを取得
 * @returns Language Client
 */
export async function getLanguageClient(): Promise<LanguageClient | undefined> {
  const extension = extensions.getExtension('groovy-lsp.groovy-lsp');
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
export async function getDefinitionAt(doc: TextDocument, position: Position): Promise<Location[]> {
  return await commands.executeCommand<Location[]>('vscode.executeDefinitionProvider', doc.uri, position);
}

/**
 * 参照の検索
 * @param doc ドキュメント
 * @param position 位置
 * @returns 参照の位置リスト
 */
export async function getReferencesAt(doc: TextDocument, position: Position): Promise<Location[]> {
  return await commands.executeCommand<Location[]>('vscode.executeReferenceProvider', doc.uri, position);
}

/**
 * ワークスペースシンボルの検索
 * @param query 検索クエリ
 * @returns シンボル情報リスト
 */
export async function getWorkspaceSymbols(query: string): Promise<SymbolInformation[]> {
  return await commands.executeCommand<SymbolInformation[]>('vscode.executeWorkspaceSymbolProvider', query);
}
