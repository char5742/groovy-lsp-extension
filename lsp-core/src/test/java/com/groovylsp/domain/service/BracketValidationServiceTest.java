package com.groovylsp.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovylsp.domain.lexer.Token;
import com.groovylsp.domain.lexer.TokenType;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.testing.FastTest;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BracketValidationServiceTest {

  private BracketValidationService service;

  @BeforeEach
  void setUp() {
    service = new BracketValidationService();
  }

  @Nested
  @DisplayName("validate メソッドのテスト")
  class ValidateTest {

    @Test
    @FastTest
    @DisplayName("正しく対応した括弧の場合、エラーが返されない")
    void testValidBrackets() {
      // given
      var tokens =
          List.of(
              new Token(TokenType.LEFT_PAREN, "(", 0, 1, 1, 1),
              new Token(TokenType.RIGHT_PAREN, ")", 1, 2, 1, 2),
              new Token(TokenType.LEFT_BRACE, "{", 3, 4, 1, 4),
              new Token(TokenType.RIGHT_BRACE, "}", 4, 5, 1, 5),
              new Token(TokenType.LEFT_BRACKET, "[", 6, 7, 1, 7),
              new Token(TokenType.RIGHT_BRACKET, "]", 7, 8, 1, 8));

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      assertTrue(result.get().isEmpty());
    }

    @Test
    @FastTest
    @DisplayName("ネストした括弧が正しく対応している場合、エラーが返されない")
    void testNestedValidBrackets() {
      // given
      var tokens =
          List.of(
              new Token(TokenType.LEFT_PAREN, "(", 0, 1, 1, 1),
              new Token(TokenType.LEFT_BRACKET, "[", 1, 2, 1, 2),
              new Token(TokenType.LEFT_BRACE, "{", 2, 3, 1, 3),
              new Token(TokenType.RIGHT_BRACE, "}", 3, 4, 1, 4),
              new Token(TokenType.RIGHT_BRACKET, "]", 4, 5, 1, 5),
              new Token(TokenType.RIGHT_PAREN, ")", 5, 6, 1, 6));

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      assertTrue(result.get().isEmpty());
    }

    @Test
    @FastTest
    @DisplayName("閉じ括弧が不足している場合、エラーが返される")
    void testMissingClosingBracket() {
      // given
      var tokens = List.of(new Token(TokenType.LEFT_PAREN, "(", 0, 1, 1, 1));

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      List<DiagnosticItem> errors = result.get();
      assertEquals(1, errors.size());
      DiagnosticItem error = errors.get(0);
      assertEquals("閉じ括弧 ')' が不足しています", error.message());
      assertEquals(0, error.startPosition().line());
      assertEquals(0, error.startPosition().character());
    }

    @Test
    @FastTest
    @DisplayName("開き括弧が不足している場合、エラーが返される")
    void testMissingOpeningBracket() {
      // given
      var tokens = List.of(new Token(TokenType.RIGHT_PAREN, ")", 0, 1, 1, 1));

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      List<DiagnosticItem> errors = result.get();
      assertEquals(1, errors.size());
      DiagnosticItem error = errors.get(0);
      assertEquals("対応する開き括弧 '(' がありません", error.message());
      assertEquals(0, error.startPosition().line());
      assertEquals(0, error.startPosition().character());
    }

    @Test
    @FastTest
    @DisplayName("括弧の種類が一致しない場合、エラーが返される")
    void testMismatchedBrackets() {
      // given
      var tokens =
          List.of(
              new Token(TokenType.LEFT_PAREN, "(", 0, 1, 1, 1),
              new Token(TokenType.RIGHT_BRACE, "}", 1, 2, 1, 2));

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      List<DiagnosticItem> errors = result.get();
      assertEquals(1, errors.size());
      DiagnosticItem error = errors.get(0);
      assertEquals("括弧の種類が一致しません: '(' に対して '}' が使用されています", error.message());
      assertEquals(0, error.startPosition().line());
      assertEquals(1, error.startPosition().character());
    }

    @Test
    @FastTest
    @DisplayName("複数のエラーが存在する場合、全てのエラーが返される")
    void testMultipleErrors() {
      // given
      var tokens =
          List.of(
              new Token(TokenType.LEFT_PAREN, "(", 0, 1, 1, 1),
              new Token(TokenType.RIGHT_BRACKET, "]", 1, 2, 1, 2),
              new Token(TokenType.LEFT_BRACE, "{", 3, 4, 2, 1));

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      List<DiagnosticItem> errors = result.get();
      assertEquals(2, errors.size());
    }

    @Test
    @FastTest
    @DisplayName("空のトークンリストの場合、エラーが返されない")
    void testEmptyTokenList() {
      // given
      var tokens = List.<Token>empty();

      // when
      Either<List<DiagnosticItem>, List<DiagnosticItem>> result = service.validate(tokens);

      // then
      assertTrue(result.isRight());
      assertTrue(result.get().isEmpty());
    }
  }
}
