import type { Hover } from 'vscode';

/**
 * ホバー結果からコンテンツテキストを抽出する共通ヘルパー関数
 * @param hovers ホバー結果の配列
 * @returns 抽出されたコンテンツ文字列
 */
export function getHoverContent(hovers: Hover[]): string {
  if (!hovers || hovers.length === 0) {
    return '';
  }

  return hovers[0].contents.map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : '')).join('');
}
