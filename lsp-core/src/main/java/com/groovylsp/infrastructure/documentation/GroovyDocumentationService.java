package com.groovylsp.infrastructure.documentation;

import com.groovylsp.domain.model.Documentation;
import com.groovylsp.domain.service.DocumentationService;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;


/**
 * GroovyDoc/JavaDocドキュメント解析サービスの実装
 *
 * <p>ASTノードからドキュメントコメントを抽出し、構造化されたドキュメント情報に変換します。
 */
@Singleton
public class GroovyDocumentationService implements DocumentationService {

  // JavaDoc/GroovyDocのタグパターン
  private static final Pattern PARAM_PATTERN = Pattern.compile("@param\\s+(\\w+)\\s+(.+?)(?=@|$)", Pattern.DOTALL);
  private static final Pattern RETURN_PATTERN = Pattern.compile("@return\\s+(.+?)(?=@|$)", Pattern.DOTALL);
  private static final Pattern THROWS_PATTERN = Pattern.compile("@throws\\s+(\\w+)\\s+(.+?)(?=@|$)", Pattern.DOTALL);
  private static final Pattern TAG_PATTERN = Pattern.compile("@(\\w+)\\s+(.+?)(?=@|$)", Pattern.DOTALL);

  private final SourceDocumentExtractor sourceExtractor;

  @Inject
  public GroovyDocumentationService(SourceDocumentExtractor sourceExtractor) {
    this.sourceExtractor = sourceExtractor;
  }

  // テスト用のコンストラクタ
  public GroovyDocumentationService() {
    this.sourceExtractor = null;
  }

  @Override
  public Option<Documentation> getDocumentation(ASTNode node) {
    if (node instanceof ClassNode classNode) {
      return getClassDocumentation();
    } else if (node instanceof MethodNode methodNode) {
      return getMethodDocumentation();
    } else if (node instanceof FieldNode fieldNode) {
      return getFieldDocumentation();
    }
    return Option.none();
  }

  @Override
  public Option<Documentation> getExternalDocumentation(String fullyQualifiedName) {
    // TODO: 外部ライブラリのドキュメント取得は後で実装
    return Option.none();
  }

  @Override
  public String formatDocumentation(Documentation documentation) {
    var sb = new StringBuilder();

    // サマリー
    if (!documentation.summary().isBlank()) {
      sb.append(documentation.summary());
    }

    // 詳細説明
    documentation.description().forEach(desc -> {
      if (!desc.isBlank()) {
        sb.append("\n\n").append(desc);
      }
    });

    // パラメータ
    if (!documentation.params().isEmpty()) {
      sb.append("\n\n**Parameters:**\n");
      for (var param : documentation.params()) {
        sb.append("- `").append(param.name()).append("`: ").append(param.description()).append("\n");
      }
    }

    // 戻り値
    documentation.returns().forEach(ret -> {
      sb.append("\n\n**Returns:** ").append(ret);
    });

    // 例外
    if (!documentation.exceptions().isEmpty()) {
      sb.append("\n\n**Throws:**\n");
      for (var exception : documentation.exceptions()) {
        sb.append("- `").append(exception.exceptionType()).append("`: ").append(exception.description()).append("\n");
      }
    }

    // その他のタグ
    if (!documentation.tags().isEmpty()) {
      sb.append("\n\n**Additional Info:**\n");
      for (var entry : documentation.tags().entrySet()) {
        String tag = entry.getKey();
        String value = entry.getValue();
        sb.append("- **").append(tag).append("**: ").append(value).append("\n");
      }
    }

    return sb.toString().trim();
  }

  @Override
  public Documentation parseDocumentationComment(String comment) {
    if (comment == null || comment.isBlank()) {
      return Documentation.empty();
    }

    // コメントの開始/終了記号を除去
    String cleaned = cleanComment(comment);

    // サマリーと詳細説明を分離
    var parts = splitSummaryAndDescription(cleaned);
    String summary = parts[0];
    String description = parts[1];

    // タグを解析
    var params = parseParams(cleaned);
    var returnDoc = parseReturn(cleaned);
    var exceptions = parseThrows(cleaned);
    var tags = parseOtherTags(cleaned);

    return new Documentation(
        summary,
        description.isBlank() ? Option.none() : Option.of(description),
        params,
        returnDoc,
        exceptions,
        tags);
  }

  private Option<Documentation> getClassDocumentation() {
    return getNodeDocComment()
        .map(this::parseDocumentationComment);
  }

  private Option<Documentation> getMethodDocumentation() {
    return getNodeDocComment()
        .map(this::parseDocumentationComment);
  }

  private Option<Documentation> getFieldDocumentation() {
    return getNodeDocComment()
        .map(this::parseDocumentationComment);
  }

  private Option<String> getNodeDocComment() {
    // SourceDocumentExtractorが利用可能な場合は使用
    if (sourceExtractor != null) {
      // TODO: ソースファイルパスの取得方法を実装
      // 現在は簡易実装
      return Option.none();
    }
    
    // フォールバック: ASTから直接取得を試みる（制限あり）
    return Option.none();
  }

  /**
   * ソースコード内容からドキュメントコメントを抽出
   *
   * @param node ASTノード
   * @param sourceContent ソースファイルの内容
   * @return ドキュメントコメント
   */
  @Override public Option<String> extractDocCommentFromSource(ASTNode node, String sourceContent) {
    if (sourceExtractor != null && sourceContent != null) {
      return sourceExtractor.extractDocCommentFromSource(sourceContent, node.getLineNumber());
    }
    return Option.none();
  }

  private String cleanComment(String comment) {
    // /** */ や /* */ を除去
    String cleaned = comment.replaceAll("^/\\*\\*?\\s*", "").replaceAll("\\s*\\*/$", "");
    
    // 各行の先頭の * を除去
    return cleaned.replaceAll("(?m)^\\s*\\*\\s?", "").trim();
  }

  private String[] splitSummaryAndDescription(String comment) {
    // 最初の文または段落をサマリーとして扱う
    String[] lines = comment.split("\\n");
    var summaryLines = new ArrayList<String>();
    var descriptionLines = new ArrayList<String>();
    
    boolean inSummary = true;
    for (String line : lines) {
      line = line.trim();
      
      // タグが見つかったら終了
      if (line.startsWith("@")) {
        break;
      }
      
      if (inSummary) {
        if (line.isEmpty()) {
          inSummary = false;
        } else {
          summaryLines.add(line);
          // 文の終わりでサマリー終了
          if (line.endsWith(".") || line.endsWith("。")) {
            inSummary = false;
          }
        }
      } else {
        if (!line.isEmpty()) {
          descriptionLines.add(line);
        }
      }
    }
    
    return new String[] {
        String.join(" ", summaryLines),
        String.join("\n", descriptionLines)
    };
  }

  private List<Documentation.ParamDoc> parseParams(String comment) {
    var params = new ArrayList<Documentation.ParamDoc>();
    Matcher matcher = PARAM_PATTERN.matcher(comment);
    
    while (matcher.find()) {
      String name = matcher.group(1);
      String description = matcher.group(2).trim().replaceAll("\\s+", " ");
      params.add(new Documentation.ParamDoc(name, description));
    }
    
    return params;
  }

  private Option<String> parseReturn(String comment) {
    Matcher matcher = RETURN_PATTERN.matcher(comment);
    if (matcher.find()) {
      return Option.of(matcher.group(1).trim().replaceAll("\\s+", " "));
    }
    return Option.none();
  }

  private List<Documentation.ThrowsDoc> parseThrows(String comment) {
    var exceptions = new ArrayList<Documentation.ThrowsDoc>();
    Matcher matcher = THROWS_PATTERN.matcher(comment);
    
    while (matcher.find()) {
      String exceptionType = matcher.group(1);
      String description = matcher.group(2).trim().replaceAll("\\s+", " ");
      exceptions.add(new Documentation.ThrowsDoc(exceptionType, description));
    }
    
    return exceptions;
  }

  private Map<String, String> parseOtherTags(String comment) {
    var tags = new HashMap<String, String>();
    Matcher matcher = TAG_PATTERN.matcher(comment);
    
    while (matcher.find()) {
      String tagName = matcher.group(1);
      // 既に処理済みのタグは除外
      if (!tagName.equals("param") && !tagName.equals("return") && !tagName.equals("throws")) {
        String value = matcher.group(2).trim().replaceAll("\\s+", " ");
        tags.put(tagName, value);
      }
    }
    
    return tags;
  }
}