package com.groovylsp.domain.repository;

import com.groovylsp.domain.model.GroovyLspConfig;
import io.vavr.control.Either;
import java.nio.file.Path;

/** 設定ファイルのリポジトリインターフェース。 */
public interface ConfigRepository {

  /**
   * 指定されたディレクトリから設定ファイルを読み込む。
   *
   * @param workspaceRoot ワークスペースのルートディレクトリ
   * @return 成功時は設定、失敗時はエラーメッセージ
   */
  Either<String, GroovyLspConfig> loadConfig(Path workspaceRoot);
}
