package com.groovylsp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.groovylsp", importOptions = ImportOption.DoNotIncludeTests.class)
public final class ArchitectureTest {

  @ArchTest
  static final ArchRule noFieldInjectionInProductionCode =
      fields()
          .should()
          .notBeAnnotatedWith("javax.inject.Inject")
          .andShould()
          .notBeAnnotatedWith("com.google.inject.Inject")
          .because(
              "Field injection should not be used in production code, use constructor injection instead");

  private ArchitectureTest() {}
}
