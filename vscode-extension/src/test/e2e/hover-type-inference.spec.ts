import { ok } from 'node:assert/strict';
import { type Extension, type Hover, commands, extensions, window, workspace } from 'vscode';
import type { ExtensionApi } from '../../types.ts';

describe('ホバー機能の型推論E2Eテスト', () => {
  let extension: Extension<ExtensionApi> | undefined;
  let editor: Awaited<ReturnType<typeof window.showTextDocument>>;

  beforeEach(async () => {
    // 拡張機能を取得して有効化
    extension = extensions.getExtension('groovy-lsp.groovy-lsp');
    if (extension && !extension.isActive) {
      await extension.activate();
    }

    // サーバーが起動するまで待機
    await new Promise((resolve) => setTimeout(resolve, 2000));
  });

  afterEach(async () => {
    // テスト後のクリーンアップ
    await commands.executeCommand('workbench.action.closeAllEditors');
  });

  it('defで宣言されたフィールドの型が初期化式から推論される', async () => {
    // テスト用のコードを作成
    const testCode = `class TestClass {
    def controller = new UserController()
    def name = "John"
    def age = 25
    def height = 180.5
    def isActive = true
    def scores = [90, 85, 92]
    def config = [enabled: true, maxSize: 100]
}

class UserController {
    String name
}`;

    // 一時ファイルを作成
    const doc = await workspace.openTextDocument({
      content: testCode,
      language: 'groovy',
    });
    editor = await window.showTextDocument(doc);

    // サーバーがファイルを処理するまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // defで宣言されたcontrollerフィールドにホバー
    const controllerIndex = testCode.indexOf('controller');
    const controllerPosition = editor.document.positionAt(controllerIndex);

    const controllerHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      controllerPosition,
    );

    ok(controllerHovers && controllerHovers.length > 0, 'controller: ホバー結果が返される必要があります');
    const controllerContent = controllerHovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    ok(
      controllerContent.includes('UserController') && !controllerContent.includes('Object'),
      'controller: UserController型として推論される必要があります（Objectではない）',
    );

    // 文字列リテラルで初期化されたフィールド
    const nameIndex = testCode.indexOf('name = "John"') + 'name'.length - 2;
    const namePosition = editor.document.positionAt(nameIndex);

    const nameHovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', doc.uri, namePosition);

    if (nameHovers && nameHovers.length > 0) {
      const nameContent = nameHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        nameContent.includes('String') || nameContent.includes('name'),
        'name: String型として推論される必要があります',
      );
    }

    // 整数リテラルで初期化されたフィールド
    const ageIndex = testCode.indexOf('age = 25') + 'age'.length - 2;
    const agePosition = editor.document.positionAt(ageIndex);

    const ageHovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', doc.uri, agePosition);

    if (ageHovers && ageHovers.length > 0) {
      const ageContent = ageHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(ageContent.includes('int') || ageContent.includes('Integer'), 'age: int型として推論される必要があります');
    }

    // リストリテラルで初期化されたフィールド
    const scoresIndex = testCode.indexOf('scores');
    const scoresPosition = editor.document.positionAt(scoresIndex);

    const scoresHovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', doc.uri, scoresPosition);

    if (scoresHovers && scoresHovers.length > 0) {
      const scoresContent = scoresHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        scoresContent.includes('ArrayList') || scoresContent.includes('List'),
        'scores: ArrayList型として推論される必要があります',
      );
    }
  });

  it('Spockのモック生成メソッドから型が推論される', async () => {
    // テスト用のSpockコード
    const testCode = `import spock.lang.Specification

class ServiceSpec extends Specification {
    def userService = Mock(UserService)
    def repository = Stub(UserRepository)
    def cache = Spy(CacheService)
    
    def "test method"() {
        given:
        def localMock = Mock(LocalService)
        
        when:
        userService.findById(1L)
        
        then:
        1 * userService.findById(_)
    }
}

interface UserService {
    User findById(Long id)
}

interface UserRepository {}
class CacheService {}
interface LocalService {}
class User {}`;

    // 一時ファイルを作成
    const doc = await workspace.openTextDocument({
      content: testCode,
      language: 'groovy',
    });
    editor = await window.showTextDocument(doc);

    // サーバーがファイルを処理するまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Mockで初期化されたフィールドにホバー
    const userServiceIndex = testCode.indexOf('userService = Mock');
    const userServicePosition = editor.document.positionAt(userServiceIndex);

    const userServiceHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      userServicePosition,
    );

    ok(userServiceHovers && userServiceHovers.length > 0, 'userService: ホバー結果が返される必要があります');
    const userServiceContent = userServiceHovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    ok(
      userServiceContent.includes('UserService') && !userServiceContent.includes('Object'),
      'userService: UserService型として推論される必要があります（Objectではない）',
    );

    // Stubで初期化されたフィールドにホバー
    const repositoryIndex = testCode.indexOf('repository = Stub');
    const repositoryPosition = editor.document.positionAt(repositoryIndex);

    const repositoryHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      repositoryPosition,
    );

    if (repositoryHovers && repositoryHovers.length > 0) {
      const repositoryContent = repositoryHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        repositoryContent.includes('UserRepository') && !repositoryContent.includes('Object'),
        'repository: UserRepository型として推論される必要があります',
      );
    }

    // Spyで初期化されたフィールドにホバー
    const cacheIndex = testCode.indexOf('cache = Spy');
    const cachePosition = editor.document.positionAt(cacheIndex);

    const cacheHovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', doc.uri, cachePosition);

    if (cacheHovers && cacheHovers.length > 0) {
      const cacheContent = cacheHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        cacheContent.includes('CacheService') && !cacheContent.includes('Object'),
        'cache: CacheService型として推論される必要があります',
      );
    }
  });

  it('メソッド内からフィールドを参照する際も型推論が適用される', async () => {
    // テスト用のコード
    const testCode = `class TestService {
    def controller = new UserController()
    def repository = Mock(UserRepository)
    def name = "ServiceName"
    
    def processRequest() {
        controller.handleRequest()  // controllerはUserController型として推論されるべき
        repository.findAll()        // repositoryはUserRepository型として推論されるべき
        def localName = name        // nameはString型として推論されるべき
        
        return controller
    }
    
    def useThis() {
        this.controller.doSomething()  // this経由でもUserController型として推論されるべき
    }
}

class UserController {
    def handleRequest() {}
    def doSomething() {}
}

interface UserRepository {
    def findAll()
}`;

    // 一時ファイルを作成
    const doc = await workspace.openTextDocument({
      content: testCode,
      language: 'groovy',
    });
    editor = await window.showTextDocument(doc);

    // サーバーがファイルを処理するまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // processRequestメソッド内のcontroller参照にホバー
    const controllerRefIndex = testCode.indexOf('controller.handleRequest');
    const controllerRefPosition = editor.document.positionAt(controllerRefIndex);

    const controllerRefHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      controllerRefPosition,
    );

    ok(controllerRefHovers && controllerRefHovers.length > 0, 'controller参照: ホバー結果が返される必要があります');
    const controllerRefContent = controllerRefHovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    ok(
      controllerRefContent.includes('UserController') && !controllerRefContent.includes('Object'),
      'controller参照: メソッド内でもUserController型として推論される必要があります',
    );

    // repository参照にホバー
    const repositoryRefIndex = testCode.indexOf('repository.findAll');
    const repositoryRefPosition = editor.document.positionAt(repositoryRefIndex);

    const repositoryRefHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      repositoryRefPosition,
    );

    if (repositoryRefHovers && repositoryRefHovers.length > 0) {
      const repositoryRefContent = repositoryRefHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        repositoryRefContent.includes('UserRepository') && !repositoryRefContent.includes('Object'),
        'repository参照: UserRepository型として推論される必要があります',
      );
    }

    // this.controller参照にホバー
    const thisControllerIndex = testCode.indexOf('this.controller.doSomething');
    const thisControllerPosition = editor.document.positionAt(thisControllerIndex + 5); // "controller"の位置

    const thisControllerHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      thisControllerPosition,
    );

    if (thisControllerHovers && thisControllerHovers.length > 0) {
      const thisControllerContent = thisControllerHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(
        thisControllerContent.includes('UserController') || thisControllerContent.includes('controller'),
        'this.controller参照: UserController型として推論される必要があります',
      );
    }
  });

  it('明示的に型が宣言されたフィールドはその型を維持する', async () => {
    // テスト用のコード
    const testCode = `class TypedFields {
    String explicitString = "value"
    int explicitInt = 42
    List<String> explicitList = ["a", "b"]
    Object explicitObject = "still object"
    private Object controller = new UserController()
}

class UserController {}`;

    // 一時ファイルを作成
    const doc = await workspace.openTextDocument({
      content: testCode,
      language: 'groovy',
    });
    editor = await window.showTextDocument(doc);

    // サーバーがファイルを処理するまで待機
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // 明示的にObject型として宣言されたcontrollerフィールドにホバー
    const controllerIndex = testCode.indexOf('controller = new UserController');
    const controllerPosition = editor.document.positionAt(controllerIndex);

    const controllerHovers = await commands.executeCommand<Hover[]>(
      'vscode.executeHoverProvider',
      doc.uri,
      controllerPosition,
    );

    ok(controllerHovers && controllerHovers.length > 0, 'controller: ホバー結果が返される必要があります');
    const controllerContent = controllerHovers[0].contents
      .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
      .join('');
    // 明示的にObjectと宣言されている場合はObjectのままであるべき
    ok(
      controllerContent.includes('Object') || controllerContent.includes('controller'),
      'controller: 明示的にObject型として宣言されている場合はObjectのままである必要があります',
    );

    // String型フィールドにホバー
    const stringIndex = testCode.indexOf('explicitString');
    const stringPosition = editor.document.positionAt(stringIndex);

    const stringHovers = await commands.executeCommand<Hover[]>('vscode.executeHoverProvider', doc.uri, stringPosition);

    if (stringHovers && stringHovers.length > 0) {
      const stringContent = stringHovers[0].contents
        .map((c) => (typeof c === 'string' ? c : 'value' in c ? c.value : ''))
        .join('');
      ok(stringContent.includes('String'), 'explicitString: String型として表示される必要があります');
    }
  });
});
