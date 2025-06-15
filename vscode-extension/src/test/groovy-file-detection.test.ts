import * as assert from 'node:assert';
import * as path from 'node:path';
import * as vscode from 'vscode';

describe('Groovyファイル判定の統合テスト', () => {
  let extension: vscode.Extension<any> | undefined;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await vscode.commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('Groovyファイルに対してのみ診断が実行される', async () => {
    // Groovyファイルを開く
    const groovyPath = path.join(__dirname, '../../test-fixtures/Test.groovy');
    const groovyDoc = await vscode.workspace.openTextDocument(groovyPath);
    await vscode.window.showTextDocument(groovyDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Groovyファイルには診断が存在することを確認
    const groovyDiagnostics = vscode.languages.getDiagnostics(groovyDoc.uri);
    assert.ok(groovyDiagnostics.length > 0, 'Groovyファイルには診断が必要です');

    // Javaファイルを開く
    const javaPath = path.join(__dirname, '../../test-fixtures/Test.java');
    const javaDoc = await vscode.workspace.openTextDocument(javaPath);
    await vscode.window.showTextDocument(javaDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Javaファイルには診断が存在しないことを確認
    const javaDiagnostics = vscode.languages.getDiagnostics(javaDoc.uri);
    assert.strictEqual(javaDiagnostics.length, 0, 'Javaファイルには診断が存在しないはずです');
  });

  it('Gradleファイルに対して診断が実行される', async () => {
    // build.gradleファイルを開く
    const gradlePath = path.join(__dirname, '../../test-fixtures/build.gradle');
    const gradleDoc = await vscode.workspace.openTextDocument(gradlePath);
    await vscode.window.showTextDocument(gradleDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Gradleファイルには診断が存在することを確認
    const gradleDiagnostics = vscode.languages.getDiagnostics(gradleDoc.uri);
    assert.ok(gradleDiagnostics.length > 0, 'Gradleファイルには診断が必要です');
  });

  it('Gradle Kotlinファイルに対して診断が実行される', async () => {
    // settings.gradle.ktsファイルを開く
    const gradleKtsPath = path.join(__dirname, '../../test-fixtures/settings.gradle.kts');
    const gradleKtsDoc = await vscode.workspace.openTextDocument(gradleKtsPath);
    await vscode.window.showTextDocument(gradleKtsDoc);

    // 診断が実行されるまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Gradle Kotlinファイルには診断が存在することを確認
    const gradleKtsDiagnostics = vscode.languages.getDiagnostics(gradleKtsDoc.uri);
    assert.ok(gradleKtsDiagnostics.length > 0, 'Gradle Kotlinファイルには診断が必要です');
  });
});
