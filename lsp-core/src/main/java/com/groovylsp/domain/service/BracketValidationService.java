package com.groovylsp.domain.service;

import com.groovylsp.domain.lexer.Token;
import com.groovylsp.domain.lexer.TokenType;
import com.groovylsp.domain.model.DiagnosticItem;
import com.groovylsp.domain.model.DiagnosticItem.DiagnosticSeverity;
import com.groovylsp.domain.model.DiagnosticItem.DocumentPosition;
import io.vavr.collection.List;
import io.vavr.control.Either;
import javax.inject.Inject;
import javax.inject.Singleton;

/** 括弧の対応をチェックするサービス */
@Singleton
public class BracketValidationService {

  @Inject
  public BracketValidationService() {}

  /**
   * トークンリストから括弧の対応をチェックし、エラーがあれば診断アイテムとして返す
   *
   * @param tokens 検証対象のトークンリスト
   * @return 括弧の対応エラーのリスト（エラーがない場合は空のリスト）
   */
  public Either<List<DiagnosticItem>, List<DiagnosticItem>> validate(List<Token> tokens) {
    var errors = List.<DiagnosticItem>empty();
    var stack = List.<Token>empty();

    for (Token token : tokens) {
      switch (token.type()) {
        case LEFT_PAREN, LEFT_BRACE, LEFT_BRACKET -> stack = stack.prepend(token);
        case RIGHT_PAREN, RIGHT_BRACE, RIGHT_BRACKET -> {
          if (stack.isEmpty()) {
            // 対応する開き括弧がない
            errors = errors.append(createMissingOpeningBracketError(token));
          } else {
            var openingToken = stack.head();
            stack = stack.tail();
            if (!isMatchingPair(openingToken.type(), token.type())) {
              // 括弧の種類が一致しない
              errors = errors.append(createMismatchedBracketError(openingToken, token));
            }
          }
        }
        default -> {
          // 括弧以外のトークンは無視
        }
      }
    }

    // スタックに残っている開き括弧は閉じ括弧が不足している
    while (!stack.isEmpty()) {
      var unclosedToken = stack.head();
      stack = stack.tail();
      errors = errors.append(createMissingClosingBracketError(unclosedToken));
    }

    return Either.right(errors);
  }

  private boolean isMatchingPair(TokenType opening, TokenType closing) {
    return (opening == TokenType.LEFT_PAREN && closing == TokenType.RIGHT_PAREN)
        || (opening == TokenType.LEFT_BRACE && closing == TokenType.RIGHT_BRACE)
        || (opening == TokenType.LEFT_BRACKET && closing == TokenType.RIGHT_BRACKET);
  }

  private DiagnosticItem createMissingClosingBracketError(Token openingToken) {
    String closingBracket = getClosingBracket(openingToken.type());
    return new DiagnosticItem(
        new DocumentPosition(openingToken.line() - 1, openingToken.column() - 1),
        new DocumentPosition(
            openingToken.line() - 1, openingToken.column() - 1 + openingToken.text().length()),
        DiagnosticSeverity.ERROR,
        String.format("閉じ括弧 '%s' が不足しています", closingBracket),
        "bracket-validation");
  }

  private DiagnosticItem createMissingOpeningBracketError(Token closingToken) {
    String openingBracket = getOpeningBracket(closingToken.type());
    return new DiagnosticItem(
        new DocumentPosition(closingToken.line() - 1, closingToken.column() - 1),
        new DocumentPosition(
            closingToken.line() - 1, closingToken.column() - 1 + closingToken.text().length()),
        DiagnosticSeverity.ERROR,
        String.format("対応する開き括弧 '%s' がありません", openingBracket),
        "bracket-validation");
  }

  private DiagnosticItem createMismatchedBracketError(Token openingToken, Token closingToken) {
    return new DiagnosticItem(
        new DocumentPosition(closingToken.line() - 1, closingToken.column() - 1),
        new DocumentPosition(
            closingToken.line() - 1, closingToken.column() - 1 + closingToken.text().length()),
        DiagnosticSeverity.ERROR,
        String.format(
            "括弧の種類が一致しません: '%s' に対して '%s' が使用されています", openingToken.text(), closingToken.text()),
        "bracket-validation");
  }

  private String getClosingBracket(TokenType openingType) {
    return switch (openingType) {
      case LEFT_PAREN -> ")";
      case LEFT_BRACE -> "}";
      case LEFT_BRACKET -> "]";
      default -> throw new IllegalArgumentException("Invalid opening bracket type: " + openingType);
    };
  }

  private String getOpeningBracket(TokenType closingType) {
    return switch (closingType) {
      case RIGHT_PAREN -> "(";
      case RIGHT_BRACE -> "{";
      case RIGHT_BRACKET -> "[";
      default -> throw new IllegalArgumentException("Invalid closing bracket type: " + closingType);
    };
  }
}
