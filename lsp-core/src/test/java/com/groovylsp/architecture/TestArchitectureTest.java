package com.groovylsp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Disabled;

@AnalyzeClasses(
    packages = "com.groovylsp",
    importOptions = ImportOption.OnlyIncludeTests.class
)
public class TestArchitectureTest {

  @ArchTest
  static final ArchRule testsShouldNotBeDisabled = 
      noMethods()
          .should()
          .beAnnotatedWith(Disabled.class)
          .because("Tests should not be disabled. Fix or remove them instead");
}