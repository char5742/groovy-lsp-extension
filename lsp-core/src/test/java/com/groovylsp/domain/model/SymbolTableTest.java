package com.groovylsp.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.testing.FastTest;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** SymbolTableの単体テスト */
@FastTest
class SymbolTableTest {

  private SymbolTable symbolTable;

  @BeforeEach
  void setUp() {
    symbolTable = new SymbolTable();
  }

  @Test
  void testAddAndFindSymbol() {
    // Given
    String uri = "file:///test.groovy";
    var range = new Range(new Position(0, 0), new Position(10, 0));
    var definition =
        new SymbolDefinition(
            "TestClass",
            "com.example.TestClass",
            SymbolKind.Class,
            uri,
            range,
            range,
            null,
            SymbolDefinition.DefinitionType.CLASS);

    // When
    symbolTable.addSymbol(definition);

    // Then
    List<SymbolDefinition> found = symbolTable.findByName("TestClass");
    assertEquals(1, found.size());
    assertEquals(definition, found.get(0));

    Option<SymbolDefinition> foundByQualified =
        symbolTable.findByQualifiedName("com.example.TestClass");
    assertTrue(foundByQualified.isDefined());
    assertEquals(definition, foundByQualified.get());
  }

  @Test
  void testGetSymbolsInFile() {
    // Given
    String uri = "file:///test.groovy";
    var range1 = new Range(new Position(0, 0), new Position(10, 0));
    var class1 =
        new SymbolDefinition(
            "Class1",
            "com.example.Class1",
            SymbolKind.Class,
            uri,
            range1,
            range1,
            null,
            SymbolDefinition.DefinitionType.CLASS);

    var range2 = new Range(new Position(15, 0), new Position(25, 0));
    var class2 =
        new SymbolDefinition(
            "Class2",
            "com.example.Class2",
            SymbolKind.Class,
            uri,
            range2,
            range2,
            null,
            SymbolDefinition.DefinitionType.CLASS);

    // When
    symbolTable.addSymbol(class1);
    symbolTable.addSymbol(class2);

    // Then
    List<SymbolDefinition> symbols = symbolTable.getSymbolsInFile(uri);
    assertEquals(2, symbols.size());
    assertTrue(symbols.contains(class1));
    assertTrue(symbols.contains(class2));
  }

  @Test
  void testClearFile() {
    // Given
    String uri1 = "file:///test1.groovy";
    String uri2 = "file:///test2.groovy";
    var range = new Range(new Position(0, 0), new Position(10, 0));

    var def1 =
        new SymbolDefinition(
            "Class1",
            "com.example.Class1",
            SymbolKind.Class,
            uri1,
            range,
            range,
            null,
            SymbolDefinition.DefinitionType.CLASS);
    var def2 =
        new SymbolDefinition(
            "Class2",
            "com.example.Class2",
            SymbolKind.Class,
            uri2,
            range,
            range,
            null,
            SymbolDefinition.DefinitionType.CLASS);

    symbolTable.addSymbol(def1);
    symbolTable.addSymbol(def2);

    // When
    symbolTable.clearFile(uri1);

    // Then
    assertTrue(symbolTable.getSymbolsInFile(uri1).isEmpty());
    assertEquals(1, symbolTable.getSymbolsInFile(uri2).size());
    assertTrue(symbolTable.findByName("Class1").isEmpty());
    assertEquals(1, symbolTable.findByName("Class2").size());
  }

  @Test
  void testFindByType() {
    // Given
    String uri = "file:///test.groovy";
    var range = new Range(new Position(0, 0), new Position(10, 0));

    var classDef =
        new SymbolDefinition(
            "TestClass",
            "com.example.TestClass",
            SymbolKind.Class,
            uri,
            range,
            range,
            null,
            SymbolDefinition.DefinitionType.CLASS);
    var methodDef =
        new SymbolDefinition(
            "testMethod",
            "com.example.TestClass.testMethod",
            SymbolKind.Method,
            uri,
            range,
            range,
            "com.example.TestClass",
            SymbolDefinition.DefinitionType.METHOD);
    var fieldDef =
        new SymbolDefinition(
            "testField",
            "com.example.TestClass.testField",
            SymbolKind.Field,
            uri,
            range,
            range,
            "com.example.TestClass",
            SymbolDefinition.DefinitionType.FIELD);

    // When
    symbolTable.addSymbol(classDef);
    symbolTable.addSymbol(methodDef);
    symbolTable.addSymbol(fieldDef);

    // Then
    List<SymbolDefinition> classes =
        symbolTable.findByType(uri, SymbolDefinition.DefinitionType.CLASS);
    assertEquals(1, classes.size());
    assertEquals(classDef, classes.get(0));

    List<SymbolDefinition> methods =
        symbolTable.findByType(uri, SymbolDefinition.DefinitionType.METHOD);
    assertEquals(1, methods.size());
    assertEquals(methodDef, methods.get(0));
  }

  @Test
  void testFindByContainingClass() {
    // Given
    String uri = "file:///test.groovy";
    var range = new Range(new Position(0, 0), new Position(10, 0));
    String className = "com.example.TestClass";

    var methodDef =
        new SymbolDefinition(
            "testMethod",
            className + ".testMethod",
            SymbolKind.Method,
            uri,
            range,
            range,
            className,
            SymbolDefinition.DefinitionType.METHOD);
    var fieldDef =
        new SymbolDefinition(
            "testField",
            className + ".testField",
            SymbolKind.Field,
            uri,
            range,
            range,
            className,
            SymbolDefinition.DefinitionType.FIELD);
    var otherClassMethod =
        new SymbolDefinition(
            "otherMethod",
            "com.example.OtherClass.otherMethod",
            SymbolKind.Method,
            uri,
            range,
            range,
            "com.example.OtherClass",
            SymbolDefinition.DefinitionType.METHOD);

    // When
    symbolTable.addSymbol(methodDef);
    symbolTable.addSymbol(fieldDef);
    symbolTable.addSymbol(otherClassMethod);

    // Then
    List<SymbolDefinition> classMembers = symbolTable.findByContainingClass(className);
    assertEquals(2, classMembers.size());
    assertTrue(classMembers.contains(methodDef));
    assertTrue(classMembers.contains(fieldDef));
    assertFalse(classMembers.contains(otherClassMethod));
  }
}
