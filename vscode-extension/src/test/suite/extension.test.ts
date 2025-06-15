import * as assert from 'node:assert';
import * as vscode from 'vscode';

describe('Extension Test Suite', () => {
  vscode.window.showInformationMessage('Start all tests.');

  it('Extension should be present @core @initialization', () => {
    assert.ok(vscode.extensions.getExtension('groovy-lsp.groovy-lsp'));
  });

  it('Extension should activate @core @activation', async () => {
    const ext = vscode.extensions.getExtension('groovy-lsp.groovy-lsp');
    assert.ok(ext);
    await ext.activate();
    assert.ok(ext.isActive);
  });

  it('Should register Groovy language @core @language-registration', () => {
    const languages = vscode.languages.getLanguages();
    languages.then((langs) => {
      assert.ok(langs.includes('groovy'));
    });
  });
});
