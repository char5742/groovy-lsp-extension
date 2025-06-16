import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import {
  type Node,
  ScriptTarget,
  createSourceFile,
  forEachChild,
  isCallExpression,
  isIdentifier,
  isStringLiteral,
} from 'typescript';

interface TestScenario {
  suite: string;
  test: string;
  file: string;
  category: 'unit' | 'e2e';
  tags?: string[];
}

class TestScenarioExtractor {
  private scenarios: TestScenario[] = [];

  extractFromFile(filePath: string): void {
    const fileContent = readFileSync(filePath, 'utf-8');
    const sourceFile = createSourceFile(filePath, fileContent, ScriptTarget.Latest, true);

    const category = this.categorizeTest(filePath);
    let currentSuite = '';

    const visit = (node: Node) => {
      this.processSuiteNode(node, (text) => {
        currentSuite = text;
      });

      this.processTestNode(node, (text) => {
        const tags = this.extractTags(text);
        this.scenarios.push({
          suite: currentSuite,
          test: text,
          file: relative(process.cwd(), filePath),
          category,
          tags: tags.length > 0 ? tags : undefined,
        });
      });

      forEachChild(node, visit);
    };

    visit(sourceFile);
  }

  private processSuiteNode(node: Node, callback: (text: string) => void): void {
    if (
      isCallExpression(node) &&
      isIdentifier(node.expression) &&
      (node.expression.text === 'suite' || node.expression.text === 'describe')
    ) {
      const firstArg = node.arguments[0];
      if (isStringLiteral(firstArg)) {
        callback(firstArg.text);
      }
    }
  }

  private processTestNode(node: Node, callback: (text: string) => void): void {
    if (
      isCallExpression(node) &&
      isIdentifier(node.expression) &&
      (node.expression.text === 'test' || node.expression.text === 'it')
    ) {
      const firstArg = node.arguments[0];
      if (isStringLiteral(firstArg)) {
        callback(firstArg.text);
      }
    }
  }

  private categorizeTest(filePath: string): 'unit' | 'e2e' {
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

  generateMarkdown(): string {
    const stats = this.calculateStats();
    let markdown = this.generateHeader(stats);
    markdown += this.generateCategorySections();
    return markdown;
  }

  private calculateStats() {
    return {
      total: this.scenarios.length,
      unit: this.scenarios.filter((s) => s.category === 'unit').length,
      e2e: this.scenarios.filter((s) => s.category === 'e2e').length,
    };
  }

  private generateHeader(stats: { total: number; unit: number; e2e: number }): string {
    let markdown = '# テストシナリオ一覧\n\n';
    markdown += '## 概要\n\n';
    markdown += `- **総テスト数**: ${stats.total}\n`;
    markdown += `- **単体テスト**: ${stats.unit}\n`;
    markdown += `- **E2Eテスト**: ${stats.e2e}\n\n`;
    return markdown;
  }

  private generateCategorySections(): string {
    let markdown = '';
    const categories: Array<'unit' | 'e2e'> = ['unit', 'e2e'];
    const categoryNames = {
      unit: '単体テスト',
      e2e: 'E2Eテスト',
    };

    for (const category of categories) {
      const categoryScenarios = this.scenarios.filter((s) => s.category === category);
      if (categoryScenarios.length === 0) {
        continue;
      }

      markdown += `## ${categoryNames[category]}\n\n`;
      markdown += this.generateSuiteSection(categoryScenarios);
    }

    return markdown;
  }

  private generateSuiteSection(scenarios: TestScenario[]): string {
    let markdown = '';
    const suites = this.groupBySuite(scenarios);

    for (const [suiteName, suiteScenarios] of suites) {
      markdown += `### ${suiteName}\n`;
      markdown += `*ファイル: ${suiteScenarios[0].file}*\n\n`;

      for (const scenario of suiteScenarios) {
        markdown += `- ${scenario.test}`;
        if (scenario.tags && scenario.tags.length > 0) {
          markdown += ` [${scenario.tags.join(', ')}]`;
        }
        markdown += '\n';
      }
      markdown += '\n';
    }

    return markdown;
  }

  private groupBySuite(scenarios: TestScenario[]): Map<string, TestScenario[]> {
    const suites = new Map<string, TestScenario[]>();
    for (const scenario of scenarios) {
      const suiteName = scenario.suite || '(No Suite)';
      if (!suites.has(suiteName)) {
        suites.set(suiteName, []);
      }
      suites.get(suiteName)?.push(scenario);
    }
    return suites;
  }

  generateJson(): string {
    return JSON.stringify(
      {
        stats: {
          total: this.scenarios.length,
          byCategory: {
            unit: this.scenarios.filter((s) => s.category === 'unit').length,
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
  const testRoot = join(__dirname, '../src/test');

  const testFiles = findTestFiles(testRoot);
  logProgress(`Found ${testFiles.length} test files`);

  for (const file of testFiles) {
    logProgress(`Extracting from: ${file}`);
    extractor.extractFromFile(file);
  }

  // 結果を出力
  const outputDir = join(__dirname, '../src/test');

  // Markdown出力
  const markdown = extractor.generateMarkdown();
  writeFileSync(join(outputDir, 'SCENARIOS.md'), markdown);
  logProgress('Generated: src/test/SCENARIOS.md');

  // JSON出力
  const json = extractor.generateJson();
  writeFileSync(join(outputDir, 'scenarios.json'), json);
  logProgress('Generated: src/test/scenarios.json');
}

// テストファイルを再帰的に検索
function findTestFiles(dir: string): string[] {
  const files: string[] = [];
  const entries = readdirSync(dir, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...findTestFiles(fullPath));
    } else if (entry.isFile() && (entry.name.endsWith('.test.ts') || entry.name.endsWith('.spec.ts'))) {
      files.push(fullPath);
    }
  }

  return files;
}

// 進捗表示用のヘルパー関数
function logProgress(message: string): void {
  // 開発スクリプトの進捗表示は意図的なもの
  process.stdout.write(`${message}\n`);
}

main().catch((error: Error) => {
  process.stderr.write(`Error: ${error.message}\n`);
  process.exit(1);
});
