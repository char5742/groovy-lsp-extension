import * as assert from 'node:assert';
import * as vscode from 'vscode';

suite('Extension Test Suite', () => {
  vscode.window.showInformationMessage('Start all tests.');

  test('Extension should be present @core @initialization', () => {
    assert.ok(vscode.extensions.getExtension('groovy-lsp.groovy-lsp'));
  });

  test('Extension should activate @core @activation', async () => {
    const ext = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    assert.ok(ext);
    await ext.activate();
    assert.ok(ext.isActive);
  });

  test('Should register Groovy language @core @language-registration', () => {
    const languages = vscode.languages.getLanguages();
    languages.then((langs) => {
      assert.ok(langs.includes('groovy'));
    });
  });
});
