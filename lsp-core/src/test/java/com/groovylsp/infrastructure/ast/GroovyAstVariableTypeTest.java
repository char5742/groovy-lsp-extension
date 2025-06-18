package com.groovylsp.infrastructure.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.testing.FastTest;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.junit.jupiter.api.Test;

/** メソッド内の変数宣言の型情報をテストする */
@FastTest
class GroovyAstVariableTypeTest {

  @Test
  void メソッド内の変数宣言の型情報を確認() {
    // given
    String content =
        """
        class TestClass {
            void testMethod() {
                def var1 = new UserController()
                UserController var2 = new UserController()
                def var3 = "string"
                String var4 = "string"
                def var5 = 123
                int var6 = 123
                def var7 = []
                List var8 = []
            }
        }

        class UserController {
            String name
        }
        """;

    var parser = new GroovyAstParser();
    var parseResult = parser.parse("test.groovy", content);

    // then
    assertTrue(parseResult.isRight());
    var classes = parseResult.get().getClasses();
    assertEquals(2, classes.size());

    ClassNode testClass = classes.get(0);
    assertEquals("TestClass", testClass.getName());

    // メソッドを取得
    MethodNode testMethod = testClass.getMethods("testMethod").get(0);
    assertNotNull(testMethod);

    // 変数宣言を解析
    var visitor = new VariableTypeVisitor();
    testMethod.getCode().visit(visitor);
  }

  /** 変数宣言の型情報を収集するビジター */
  private static class VariableTypeVisitor extends ClassCodeVisitorSupport {
    @Override
    protected SourceUnit getSourceUnit() {
      return null;
    }

    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
      Expression leftExpression = expression.getLeftExpression();
      Expression rightExpression = expression.getRightExpression();

      if (leftExpression instanceof VariableExpression) {
        var varExpr = (VariableExpression) leftExpression;
        System.out.println("=== 変数宣言: " + varExpr.getName() + " ===");
        System.out.println("宣言された型: " + varExpr.getType().getName());
        System.out.println("型のテキスト: " + varExpr.getType().getText());
        System.out.println("isDynamicTyped: " + varExpr.isDynamicTyped());

        if (rightExpression != null) {
          System.out.println("右辺の式の型: " + rightExpression.getClass().getSimpleName());
          System.out.println("右辺の型: " + rightExpression.getType().getName());

          if (rightExpression instanceof ConstructorCallExpression) {
            var ctorCall = (ConstructorCallExpression) rightExpression;
            System.out.println("コンストラクタ呼び出しの型: " + ctorCall.getType().getName());
          }
        }
        System.out.println();
      }

      super.visitDeclarationExpression(expression);
    }

    @Override
    public void visitBlockStatement(BlockStatement block) {
      System.out.println("=== ブロックステートメント開始 ===");
      super.visitBlockStatement(block);
      System.out.println("=== ブロックステートメント終了 ===\n");
    }
  }

  @Test
  void フィールドと変数宣言の型情報の違いを確認() {
    // given
    String content =
        """
        class TestClass {
            def field1 = new UserController()
            UserController field2 = new UserController()

            void testMethod() {
                def var1 = new UserController()
                UserController var2 = new UserController()
            }
        }

        class UserController {}
        """;

    var parser = new GroovyAstParser();
    var parseResult = parser.parse("test.groovy", content);

    // then
    assertTrue(parseResult.isRight());
    var classes = parseResult.get().getClasses();
    ClassNode testClass = classes.get(0);

    // フィールドの型情報を確認
    System.out.println("=== フィールドの型情報 ===");
    for (var field : testClass.getFields()) {
      System.out.println("フィールド: " + field.getName() + " - 型: " + field.getType().getName());
      if (field.hasInitialExpression()) {
        Expression init = field.getInitialExpression();
        System.out.println("  初期化式の型: " + init.getType().getName());
        if (init instanceof ConstructorCallExpression) {
          var ctorCall = (ConstructorCallExpression) init;
          System.out.println("  コンストラクタ呼び出しの型: " + ctorCall.getType().getName());
        }
      }
    }

    // メソッド内の変数宣言を確認
    System.out.println("\n=== メソッド内の変数宣言 ===");
    MethodNode testMethod = testClass.getMethods("testMethod").get(0);
    var visitor = new VariableTypeVisitor();
    testMethod.getCode().visit(visitor);
  }

  @Test
  void MockフィールドとSpockメソッド呼び出しの型情報を確認() {
    // given
    String content =
        """
        import spock.lang.Specification

        interface UserService {
            User findById(Long id)
        }

        class User {
            Long id
            String name
        }

        class UserController {
            private UserService userService

            UserController(UserService userService) {
                this.userService = userService
            }

            User getUser(Long id) {
                return userService.findById(id)
            }
        }

        class MockingExampleSpec extends Specification {
            def userService = Mock(UserService)
            def controller = new UserController(userService)

            def "test mock"() {
                when:
                def result = controller.getUser(1L)

                then:
                result != null
            }
        }
        """;

    var parser = new GroovyAstParser();
    var parseResult = parser.parse("test.groovy", content);

    // then
    assertTrue(parseResult.isRight());
    var classes = parseResult.get().getClasses();
    ClassNode specClass = null;
    for (ClassNode clazz : classes) {
      if (clazz.getName().endsWith("MockingExampleSpec")) {
        specClass = clazz;
        break;
      }
    }
    assertNotNull(specClass);

    // フィールドの型情報を確認
    System.out.println("=== Spockテストクラスのフィールドの型情報 ===");
    for (var field : specClass.getFields()) {
      System.out.println("フィールド: " + field.getName() + " - 型: " + field.getType().getName());
      if (field.hasInitialExpression()) {
        Expression init = field.getInitialExpression();
        System.out.println("  初期化式のクラス: " + init.getClass().getSimpleName());
        System.out.println("  初期化式の型: " + init.getType().getName());
      }
    }
  }
}
