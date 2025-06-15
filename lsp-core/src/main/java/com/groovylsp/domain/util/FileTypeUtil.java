package com.groovylsp.domain.util;

import java.net.URI;
import java.util.Set;

/** ファイルタイプ判定のユーティリティクラス。 */
public final class FileTypeUtil {

  /** Groovyファイルの拡張子セット。 */
  private static final Set<String> GROOVY_EXTENSIONS = Set.of(".groovy", ".gradle", ".gradle.kts");

  private FileTypeUtil() {
    // ユーティリティクラスのインスタンス化を防ぐ
  }

  /**
   * 指定されたURIがGroovyファイルかどうかを判定する。
   *
   * @param uri 判定対象のURI
   * @return Groovyファイルの場合はtrue、それ以外はfalse
   */
  public static boolean isGroovyFile(URI uri) {
    if (uri == null) {
      return false;
    }

    var path = uri.getPath();
    if (path == null || path.isEmpty()) {
      return false;
    }

    // ファイル名を取得（最後の/以降）
    var fileName = path.substring(path.lastIndexOf('/') + 1);
    if (fileName.isEmpty()) {
      return false;
    }

    var lowerCaseFileName = fileName.toLowerCase();
    return GROOVY_EXTENSIONS.stream()
        .anyMatch(ext -> lowerCaseFileName.endsWith(ext) && !lowerCaseFileName.equals(ext));
  }
}
