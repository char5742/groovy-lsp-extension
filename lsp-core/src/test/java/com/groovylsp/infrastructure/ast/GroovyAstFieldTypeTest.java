package com.groovylsp.infrastructure.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.infrastructure.parser.GroovyAstParser;
import com.groovylsp.testing.FastTest;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.junit.jupiter.api.Test;

/** Groovy ASTのフィールド型情報取得のテスト */
@FastTest
class GroovyAstFieldTypeTest {

  @Test
  void フィールドの型情報を詳細に確認() {
    // given
    String content =
        """
        class Person {
            private String name = "John"
            String surname
            int age = 25
            def dynamic = "value"
            List<String> hobbies = ["reading", "swimming"]
            Map<String, Integer> scores = [math: 90, english: 85]
        }
        """;

    var parser = new GroovyAstParser();
    var parseResult = parser.parse("test.groovy", content);

    // then
    assertTrue(parseResult.isRight());
    var classes = parseResult.get().getClasses();
    assertEquals(1, classes.size());

    ClassNode personClass = classes.get(0);
    assertEquals("Person", personClass.getName());

    // フィールド情報を詳細に出力
    for (FieldNode field : personClass.getFields()) {
      System.out.println("=== Field: " + field.getName() + " ===");
      System.out.println("Type: " + field.getType());
      System.out.println("Type name: " + field.getType().getName());
      System.out.println("Type name without package: " + field.getType().getNameWithoutPackage());
      System.out.println("Is array: " + field.getType().isArray());
      System.out.println("Has generics: " + (field.getType().getGenericsTypes() != null));
      if (field.getType().getGenericsTypes() != null) {
        System.out.println("Generics count: " + field.getType().getGenericsTypes().length);
      }
      System.out.println("Type text: " + field.getType().getText());
      System.out.println("Type toString: " + field.getType().toString());
      System.out.println("Has initial expression: " + field.hasInitialExpression());
      if (field.hasInitialExpression()) {
        System.out.println("Initial expression type: " + field.getInitialExpression().getType());
        System.out.println("Initial expression text: " + field.getInitialExpression().getText());
      }
      System.out.println();
    }

    // 個別のフィールドを確認
    FieldNode nameField = personClass.getField("name");
    assertNotNull(nameField);
    assertEquals("String", nameField.getType().getName()); // Groovyは単純名を返す
    assertEquals("String", nameField.getType().getNameWithoutPackage());

    FieldNode dynamicField = personClass.getField("dynamic");
    assertNotNull(dynamicField);
    System.out.println("Dynamic field type: " + dynamicField.getType().getName());
    // defで宣言されたフィールドの型を確認

    FieldNode hobbiesField = personClass.getField("hobbies");
    assertNotNull(hobbiesField);
    System.out.println("Hobbies field type: " + hobbiesField.getType().getName());
    System.out.println("Hobbies field type text: " + hobbiesField.getType().getText());
  }

  @Test
  void 型推論を使った宣言の型情報を確認() {
    // given
    String content =
        """
        class TestClass {
            def name = "John"
            def age = 25
            def list = []
            def map = [:]
            def nullValue = null
        }
        """;

    var parser = new GroovyAstParser();
    var parseResult = parser.parse("test.groovy", content);

    // then
    assertTrue(parseResult.isRight());
    var classes = parseResult.get().getClasses();
    ClassNode testClass = classes.get(0);

    // 各フィールドの型情報を出力
    for (FieldNode field : testClass.getFields()) {
      System.out.println("Field: " + field.getName() + " - Type: " + field.getType().getName());
      if (field.hasInitialExpression()) {
        System.out.println(
            "  Initial expression type: " + field.getInitialExpression().getType().getName());
      }
    }
  }
}
