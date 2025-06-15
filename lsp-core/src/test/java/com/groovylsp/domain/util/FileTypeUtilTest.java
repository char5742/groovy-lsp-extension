package com.groovylsp.domain.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.testing.FastTest;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FileTypeUtilのテスト")
class FileTypeUtilTest {

  @Nested
  @DisplayName("isGroovyFileメソッドのテスト")
  class IsGroovyFileTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "file:///test/Main.groovy",
          "file:///test/Test.GROOVY",
          "file:///test/build.gradle",
          "file:///test/BUILD.GRADLE",
          "file:///test/settings.gradle.kts",
          "file:///test/SETTINGS.GRADLE.KTS",
          "file:///path/with/spaces/test.groovy",
          "file:///deep/nested/path/to/file.gradle"
        })
    @FastTest
    @DisplayName("Groovyファイルの場合はtrueを返す")
    void returnsTrueForGroovyFiles(String uriString) throws URISyntaxException {
      var uri = new URI(uriString);
      assertTrue(FileTypeUtil.isGroovyFile(uri));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "file:///test/Main.java",
          "file:///test/Test.kt",
          "file:///test/build.xml",
          "file:///test/pom.xml",
          "file:///test/README.md",
          "file:///test/config.json",
          "file:///test/noextension",
          "file:///test/.groovy", // ファイル名が拡張子のみの場合
          "file:///test/groovy", // 拡張子なし
          "file:///test/file.groovy.bak" // .groovyが途中にある場合
        })
    @FastTest
    @DisplayName("Groovyファイル以外の場合はfalseを返す")
    void returnsFalseForNonGroovyFiles(String uriString) throws URISyntaxException {
      var uri = new URI(uriString);
      assertFalse(FileTypeUtil.isGroovyFile(uri));
    }

    @Test
    @FastTest
    @DisplayName("nullの場合はfalseを返す")
    void returnsFalseForNull() {
      assertFalse(FileTypeUtil.isGroovyFile(null));
    }

    @Test
    @FastTest
    @DisplayName("パスが空のURIの場合はfalseを返す")
    void returnsFalseForEmptyPath() throws URISyntaxException {
      var uri = new URI("file:///");
      assertFalse(FileTypeUtil.isGroovyFile(uri));
    }

    @Test
    @FastTest
    @DisplayName("opaqueなURIの場合はfalseを返す")
    void returnsFalseForOpaqueUri() throws URISyntaxException {
      var uri = new URI("mailto:test@example.com");
      assertFalse(FileTypeUtil.isGroovyFile(uri));
    }
  }
}
