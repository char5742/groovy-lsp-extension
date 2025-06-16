package com.groovylsp.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovylsp.domain.model.TextDocument;
import com.groovylsp.domain.repository.TextDocumentRepository;
import com.groovylsp.domain.service.TypeInfoService;
import com.groovylsp.testing.FastTest;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.net.URI;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HoverUseCaseのテスト */
@FastTest
class HoverUseCaseTest {

  private TextDocumentRepository repository;
  private TypeInfoService typeInfoService;
  private HoverUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = mock(TextDocumentRepository.class);
    typeInfoService = mock(TypeInfoService.class);
    useCase = new HoverUseCase(repository, typeInfoService);
  }

  @Test
  @DisplayName("型情報がある場合はMarkdownで表示される")
  void getHoverWithTypeInfo() {
    // given
    String uri = "file:///test/Calculator.groovy";
    String content = "class Calculator { int value = 10 }";
    var position = new Position(0, 23); // "value"の位置

    var params = new HoverParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));
    params.setPosition(position);

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));

    var typeInfo =
        new TypeInfoService.TypeInfo(
            "value", "int", TypeInfoService.TypeInfo.Kind.FIELD, null, "private");
    when(typeInfoService.getTypeInfoAt(uri, content, position)).thenReturn(Either.right(typeInfo));

    // when
    Either<String, Hover> result = useCase.getHover(params);

    // then
    assertThat(result.isRight()).isTrue();
    Hover hover = result.get();
    assertThat(hover).isNotNull();

    assertThat(hover.getContents().isRight()).isTrue();
    MarkupContent contents = hover.getContents().getRight();
    assertThat(contents.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(contents.getValue()).contains("```groovy");
    assertThat(contents.getValue()).contains("private int value");
    assertThat(contents.getValue()).contains("```");
  }

  @Test
  @DisplayName("ドキュメントが見つからない場合はエラーを返す")
  void getHoverDocumentNotFound() {
    // given
    String uri = "file:///test/NotFound.groovy";

    var params = new HoverParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));
    params.setPosition(new Position(0, 0));

    when(repository.findByUri(URI.create(uri))).thenReturn(Option.none());

    // when
    Either<String, Hover> result = useCase.getHover(params);

    // then
    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).contains("ドキュメントが見つかりません: " + uri);
  }

  @Test
  @DisplayName("型情報が見つからない場合はデフォルトテキストを返す")
  void getHoverWithoutTypeInfo() {
    // given
    String uri = "file:///test/Script.groovy";
    String content = "def hello() { println 'Hello' }";
    var position = new Position(0, 15); // println の位置

    var params = new HoverParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));
    params.setPosition(position);

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));
    when(typeInfoService.getTypeInfoAt(uri, content, position))
        .thenReturn(Either.left("型情報が見つかりません"));

    // when
    Either<String, Hover> result = useCase.getHover(params);

    // then
    assertThat(result.isRight()).isTrue();
    Hover hover = result.get();
    assertThat(hover.getContents().isRight()).isTrue();
    MarkupContent contents = hover.getContents().getRight();
    assertThat(contents.getKind()).isEqualTo(MarkupKind.PLAINTEXT);
    assertThat(contents.getValue()).isEqualTo("Groovy element");
  }

  @Test
  @DisplayName("メソッドの型情報が正しく表示される")
  void getHoverForMethod() {
    // given
    String uri = "file:///test/Calculator.groovy";
    String content = "class Calculator { int add(int a, int b) { return a + b } }";
    var position = new Position(0, 23); // "add"の位置

    var params = new HoverParams();
    params.setTextDocument(new TextDocumentIdentifier(uri));
    params.setPosition(position);

    var document = new TextDocument(URI.create(uri), "groovy", 1, content);
    when(repository.findByUri(URI.create(uri))).thenReturn(Option.of(document));

    var typeInfo =
        new TypeInfoService.TypeInfo(
            "add", "add(int a, int b): int", TypeInfoService.TypeInfo.Kind.METHOD, null, "public");
    when(typeInfoService.getTypeInfoAt(uri, content, position)).thenReturn(Either.right(typeInfo));

    // when
    Either<String, Hover> result = useCase.getHover(params);

    // then
    assertThat(result.isRight()).isTrue();
    Hover hover = result.get();
    assertThat(hover).isNotNull();

    assertThat(hover.getContents().isRight()).isTrue();
    MarkupContent contents = hover.getContents().getRight();
    assertThat(contents.getKind()).isEqualTo(MarkupKind.MARKDOWN);
    assertThat(contents.getValue()).contains("```groovy");
    assertThat(contents.getValue()).contains("public add(int a, int b): int");
    assertThat(contents.getValue()).contains("```");
  }
}
