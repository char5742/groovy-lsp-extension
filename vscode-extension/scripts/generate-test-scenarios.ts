// biome-ignore lint/style/noNamespaceImport: スクリプトファイルでは標準的な書き方
// biome-ignore lint/correctness/noNodejsModules: Node.jsスクリプト
import * as fs from 'node:fs';
// biome-ignore lint/style/noNamespaceImport: スクリプトファイルでは標準的な書き方
// biome-ignore lint/correctness/noNodejsModules: Node.jsスクリプト
import * as path from 'node:path';
// biome-ignore lint/style/noNamespaceImport: TypeScript APIは名前空間での使用が一般的
import * as ts from 'typescript';

interface TestScenario {
  suite: string;
  test: string;
  file: string;
  category: 'unit' | 'integration' | 'e2e';
  tags?: string[];
}

class TestScenarioExtractor {
  private scenarios: TestScenario[] = [];

  extractFromFile(filePath: string): void {
    const fileContent = fs.readFileSync(filePath, 'utf-8');
    const sourceFile = ts.createSourceFile(filePath, fileContent, ts.ScriptTarget.Latest, true);

    const category = this.categorizeTest(filePath);
    let currentSuite = '';

    // biome-ignore lint/complexity/noExcessiveCognitiveComplexity: AST走査の特性上、複雑度が高くなる
    const visit = (node: ts.Node) => {
      // suite() or describe() calls
      if (
        ts.isCallExpression(node) &&
        ts.isIdentifier(node.expression) &&
        (node.expression.text === 'suite' || node.expression.text === 'describe')
      ) {
        const firstArg = node.arguments[0];
        if (ts.isStringLiteral(firstArg)) {
          currentSuite = firstArg.text;
        }
      }

      // test() or it() calls
      if (
        ts.isCallExpression(node) &&
        ts.isIdentifier(node.expression) &&
        (node.expression.text === 'test' || node.expression.text === 'it')
      ) {
        const firstArg = node.arguments[0];
        if (ts.isStringLiteral(firstArg)) {
          const tags = this.extractTags(firstArg.text);
          this.scenarios.push({
            suite: currentSuite,
            test: firstArg.text,
            file: path.relative(process.cwd(), filePath),
            category,
            tags: tags.length > 0 ? tags : undefined,
          });
        }
      }

      ts.forEachChild(node, visit);
    };

    visit(sourceFile);
  }

  private categorizeTest(filePath: string): 'unit' | 'integration' | 'e2e' {
    if (filePath.includes('/integration/')) {
      return 'integration';
    }
    if (filePath.includes('/e2e/')) {
      return 'e2e';
    }
    return 'unit';
  }

  private extractTags(testName: string): string[] {
    const tagPattern = /@(\w+)/g;
    const matches = testName.match(tagPattern);
    return matches ? matches.map((tag) => tag.substring(1)) : [];
  }

  // biome-ignore lint/complexity/noExcessiveCognitiveComplexity: レポート生成の特性上、複雑度が高くなる
  generateMarkdown(): string {
    let markdown = '# テストシナリオ一覧\n\n';

    // 統計情報
    const stats = {
      total: this.scenarios.length,
      unit: this.scenarios.filter((s) => s.category === 'unit').length,
      integration: this.scenarios.filter((s) => s.category === 'integration').length,
      e2e: this.scenarios.filter((s) => s.category === 'e2e').length,
    };

    markdown += '## 概要\n\n';
    markdown += `- **総テスト数**: ${stats.total}\n`;
    markdown += `- **単体テスト**: ${stats.unit}\n`;
    markdown += `- **統合テスト**: ${stats.integration}\n`;
    markdown += `- **E2Eテスト**: ${stats.e2e}\n\n`;

    // カテゴリ別セクション
    const categories: Array<'unit' | 'integration' | 'e2e'> = ['unit', 'integration', 'e2e'];
    const categoryNames = {
      unit: '単体テスト',
      integration: '統合テスト',
      e2e: 'E2Eテスト',
    };

    for (const category of categories) {
      const categoryScenarios = this.scenarios.filter((s) => s.category === category);
      if (categoryScenarios.length === 0) {
        continue;
      }

      markdown += `## ${categoryNames[category]}\n\n`;

      // スイートごとにグループ化
      const suites = new Map<string, TestScenario[]>();
      for (const scenario of categoryScenarios) {
        const suiteName = scenario.suite || '(No Suite)';
        if (!suites.has(suiteName)) {
          suites.set(suiteName, []);
        }
        suites.get(suiteName)?.push(scenario);
      }

      // 各スイートを出力
      for (const [suiteName, scenarios] of suites) {
        markdown += `### ${suiteName}\n`;
        markdown += `*ファイル: ${scenarios[0].file}*\n\n`;

        for (const scenario of scenarios) {
          markdown += `- ${scenario.test}`;
          if (scenario.tags && scenario.tags.length > 0) {
            markdown += ` [${scenario.tags.join(', ')}]`;
          }
          markdown += '\n';
        }
        markdown += '\n';
      }
    }

    return markdown;
  }

  generateJson(): string {
    return JSON.stringify(
      {
        stats: {
          total: this.scenarios.length,
          byCategory: {
            unit: this.scenarios.filter((s) => s.category === 'unit').length,
            integration: this.scenarios.filter((s) => s.category === 'integration').length,
            e2e: this.scenarios.filter((s) => s.category === 'e2e').length,
          },
        },
        scenarios: this.scenarios,
      },
      null,
      2,
    );
  }
}

// メイン処理
async function main() {
  const extractor = new TestScenarioExtractor();
  const testRoot = path.join(__dirname, '../src/test');

  // テストファイルを再帰的に検索
  function findTestFiles(dir: string): string[] {
    const files: string[] = [];
    const entries = fs.readdirSync(dir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        files.push(...findTestFiles(fullPath));
      } else if (entry.isFile() && (entry.name.endsWith('.test.ts') || entry.name.endsWith('.spec.ts'))) {
        files.push(fullPath);
      }
    }

    return files;
  }

  const testFiles = findTestFiles(testRoot);
  // biome-ignore lint/suspicious/noConsole lint/suspicious/noConsoleLog: 開発スクリプトの進捗表示
  console.log(`Found ${testFiles.length} test files`);

  for (const file of testFiles) {
    // biome-ignore lint/suspicious/noConsole lint/suspicious/noConsoleLog: 開発スクリプトの進捗表示
    console.log(`Extracting from: ${file}`);
    extractor.extractFromFile(file);
  }

  // 結果を出力
  const outputDir = path.join(__dirname, '../src/test');

  // Markdown出力
  const markdown = extractor.generateMarkdown();
  fs.writeFileSync(path.join(outputDir, 'SCENARIOS.md'), markdown);
  // biome-ignore lint/suspicious/noConsole lint/suspicious/noConsoleLog: 開発スクリプトの進捗表示
  console.log('Generated: src/test/SCENARIOS.md');

  // JSON出力
  const json = extractor.generateJson();
  fs.writeFileSync(path.join(outputDir, 'scenarios.json'), json);
  // biome-ignore lint/suspicious/noConsole lint/suspicious/noConsoleLog: 開発スクリプトの進捗表示
  console.log('Generated: src/test/scenarios.json');
}

main().catch(console.error);
