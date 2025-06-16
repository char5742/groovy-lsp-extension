import { ok, strictEqual } from 'node:assert/strict';
import { join } from 'node:path';
import { type Extension, commands, extensions, languages, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types';

describe('Groovyファイル判定の統合テスト', () => {
  let extension: Extension<ExtensionApi> | undefined;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('Groovyファイルに対してのみ診断が実行される', async () => {
    // Groovyファイルを開く
    const groovyPath = join(__dirname, '../../../test-fixtures/Test.groovy');
    const groovyDoc = await workspace.openTextDocument(groovyPath);
    await window.showTextDocument(groovyDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Groovyファイルには診断が存在することを確認
    const groovyDiagnostics = languages.getDiagnostics(groovyDoc.uri);
    ok(groovyDiagnostics.length > 0, 'Groovyファイルには診断が必要です');

    // Javaファイルを開く
    const javaPath = join(__dirname, '../../../test-fixtures/Test.java');
    const javaDoc = await workspace.openTextDocument(javaPath);
    await window.showTextDocument(javaDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Javaファイルには診断が存在しないことを確認
    const javaDiagnostics = languages.getDiagnostics(javaDoc.uri);
    strictEqual(javaDiagnostics.length, 0, 'Javaファイルには診断が存在しないはずです');
  });

  it('Gradleファイルに対して診断が実行される', async () => {
    // build.gradleファイルを開く
    const gradlePath = join(__dirname, '../../../test-fixtures/build.gradle');
    const gradleDoc = await workspace.openTextDocument(gradlePath);
    await window.showTextDocument(gradleDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Gradleファイルには診断が存在することを確認
    const gradleDiagnostics = languages.getDiagnostics(gradleDoc.uri);
    ok(gradleDiagnostics.length > 0, 'Gradleファイルには診断が必要です');
  });

  it('Gradle Kotlinファイルに対して診断が実行される', async () => {
    // settings.gradle.ktsファイルを開く
    const gradleKtsPath = join(__dirname, '../../../test-fixtures/settings.gradle.kts');
    const gradleKtsDoc = await workspace.openTextDocument(gradleKtsPath);
    await window.showTextDocument(gradleKtsDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Gradle Kotlinファイルには診断が存在することを確認
    const gradleKtsDiagnostics = languages.getDiagnostics(gradleKtsDoc.uri);
    ok(gradleKtsDiagnostics.length > 0, 'Gradle Kotlinファイルには診断が必要です');
  });
});
