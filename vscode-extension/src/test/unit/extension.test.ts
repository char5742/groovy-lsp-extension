import { ok } from 'node:assert/strict';
import { extensions, languages, window } from 'vscode';

describe('Extension Test Suite', () => {
  window.showInformationMessage('Start all tests.');

  it('Extension should be present @core @initialization', () => {
    ok(extensions.getExtension('groovy-lsp.groovy-lsp'));
  });

  it('Extension should activate @core @activation', async () => {
    const ext = extensions.getExtension('groovy-lsp.groovy-lsp');
    ok(ext);
    await ext.activate();
    ok(ext.isActive);
  });

  it('Should register Groovy language @core @language-registration', async () => {
    const langs = await languages.getLanguages();
    ok(langs.includes('groovy'));
  });
});
