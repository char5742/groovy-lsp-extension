package com.groovylsp.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovylsp.testing.FastTest;
import org.junit.jupiter.api.Test;

@FastTest
class LineCountResultTest {

  @Test
  void 正常な行カウント結果を作成できる() {
    var result = new LineCountResult(10, 2, 3, 5);

    assertThat(result.totalLines()).isEqualTo(10);
    assertThat(result.blankLines()).isEqualTo(2);
    assertThat(result.commentLines()).isEqualTo(3);
    assertThat(result.codeLines()).isEqualTo(5);
  }

  @Test
  void 空の行カウント結果を作成できる() {
    var result = LineCountResult.empty();

    assertThat(result.totalLines()).isEqualTo(0);
    assertThat(result.blankLines()).isEqualTo(0);
    assertThat(result.commentLines()).isEqualTo(0);
    assertThat(result.codeLines()).isEqualTo(0);
  }

  @Test
  void フォーマットされた文字列を取得できる() {
    var result = new LineCountResult(10, 2, 3, 5);

    assertThat(result.toFormattedString()).isEqualTo("総行数: 10行 (コード: 5行, 空行: 2行, コメント: 3行)");
  }

  @Test
  void 総行数が負の値の場合は例外が発生する() {
    assertThatThrownBy(() -> new LineCountResult(-1, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("総行数は負の値になりません");
  }

  @Test
  void 空行数が負の値の場合は例外が発生する() {
    assertThatThrownBy(() -> new LineCountResult(1, -1, 0, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("空行数は負の値になりません");
  }

  @Test
  void コメント行数が負の値の場合は例外が発生する() {
    assertThatThrownBy(() -> new LineCountResult(1, 0, -1, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("コメント行数は負の値になりません");
  }

  @Test
  void コード行数が負の値の場合は例外が発生する() {
    assertThatThrownBy(() -> new LineCountResult(1, 0, 0, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("コード行数は負の値になりません");
  }

  @Test
  void 行数の合計が一致しない場合は例外が発生する() {
    assertThatThrownBy(() -> new LineCountResult(10, 2, 3, 6))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("行数の合計が一致しません");
  }
}
