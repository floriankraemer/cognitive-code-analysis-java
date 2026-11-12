package net.floriankraemer.cognitive_analysis.domain;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/**
 * Parses Java source code and measures the cognitive metrics of every method and constructor.
 *
 * <p>This is the only Java-specific part of the analysis. It is stateless and thread-safe.
 *
 * <p>Units are the methods and constructors of top-level and member classes, enums, records and
 * interfaces. Interface methods count only when they have a body; abstract and native class methods
 * count with empty metrics. Anonymous and local classes are skipped; lambdas belong to their method.
 * A class is keyed by its fully qualified name with nested classes separated by dots, a method by
 * {@code pkg.Outer.Inner::method}. Constructors are named after their class. When a class declares
 * several methods with the same name, all of them get their erased parameter types appended, as in
 * {@code method(List,String...)}. A {@value #IGNORE_TAG} comment before a declaration skips it.
 */
public class CognitiveMetricsCollector {

  static final String IGNORE_TAG = "@cca-ignore";
  private static final LanguageLevel LANGUAGE_LEVEL = LanguageLevel.JAVA_26;

  /**
   * @return the metrics per method key, in source order
   * @throws SourceParseException when the code is not valid Java
   */
  public Map<String, CognitiveMetrics> collect(final String code) throws SourceParseException {
    final ParseResult<CompilationUnit> result =
        new JavaParser(new ParserConfiguration().setLanguageLevel(LANGUAGE_LEVEL)).parse(code);
    if (!result.isSuccessful()) {
      throw new SourceParseException(result.getProblems().get(0).getVerboseMessage());
    }
    return new FileScan(result.getResult().orElseThrow()).scan();
  }

  /** The state of collecting one file. */
  private static final class FileScan {

    private final CompilationUnit unit;
    private final List<Comment> ignoreComments;
    private final Map<String, CognitiveMetrics> metrics = new LinkedHashMap<>();

    FileScan(final CompilationUnit unit) {
      this.unit = unit;
      this.ignoreComments = unit.getAllComments().stream()
          .filter(comment -> comment.getContent().contains(IGNORE_TAG))
          .toList();
    }

    Map<String, CognitiveMetrics> scan() {
      final String packagePrefix =
          unit.getPackageDeclaration().map(declaration -> declaration.getNameAsString() + ".").orElse("");
      unit.getTypes().forEach(type -> scanType(type, packagePrefix + type.getNameAsString()));
      return metrics;
    }

    private void scanType(final TypeDeclaration<?> type, final String className) {
      if (isIgnored(type, type.getName())) {
        return;
      }
      final Map<String, Long> nameCounts = type.getMembers().stream()
          .map(member -> unitOf(type, member))
          .flatMap(Optional::stream)
          .collect(groupingBy(Unit::name, counting()));
      for (final BodyDeclaration<?> member : type.getMembers()) {
        if (member instanceof TypeDeclaration<?> nested) {
          scanType(nested, className + "." + nested.getNameAsString());
          continue;
        }
        final Optional<Unit> callable = unitOf(type, member);
        if (callable.isPresent() && !isIgnored(member, callable.get().simpleName())) {
          final Unit found = callable.get();
          final String methodName =
              nameCounts.get(found.name()) > 1 ? found.name() + signature(found.parameters()) : found.name();
          metrics.putIfAbsent(className + "::" + methodName, measure(className, methodName, found));
        }
      }
    }

    /**
     * A declaration is ignored when the tag is in its attached comment or in any comment between the
     * previous sibling and its name, which covers stacked comments and comments between annotations.
     */
    private boolean isIgnored(final Node declaration, final SimpleName name) {
      final Optional<Position> lowerBound = previousSiblingEnd(declaration);
      final Position nameBegin = begin(name);
      return ignoreComments.stream().anyMatch(comment ->
          declaration.getComment().filter(attached -> attached == comment).isPresent()
              || startsAfterLineOf(comment, lowerBound) && begin(comment).isBefore(nameBegin));
    }

    /** A comment on the line a sibling ends on trails that sibling, so it must start on a later line. */
    private static boolean startsAfterLineOf(final Comment comment, final Optional<Position> lowerBound) {
      return lowerBound.map(bound -> begin(comment).line > bound.line).orElse(true);
    }

    private static Optional<Position> previousSiblingEnd(final Node declaration) {
      final Position declarationBegin = begin(declaration);
      return declaration.getParentNode().orElseThrow().getChildNodes().stream()
          .filter(sibling -> !(sibling instanceof Comment))
          .map(sibling -> sibling.getRange().orElseThrow().end)
          .filter(end -> end.isBefore(declarationBegin))
          .max(Position::compareTo);
    }

    private static Position begin(final Node node) {
      return node.getBegin().orElseThrow();
    }
  }

  /** A method or constructor with the name it is reported under. */
  private record Unit(
      BodyDeclaration<?> declaration,
      String name,
      SimpleName simpleName,
      NodeList<Parameter> parameters,
      Optional<BlockStmt> body) {}

  private static Optional<Unit> unitOf(final TypeDeclaration<?> type, final BodyDeclaration<?> member) {
    final String typeName = type.getNameAsString();
    return switch (member) {
      case MethodDeclaration method when method.getBody().isPresent() || !isInterface(type) -> Optional.of(
          new Unit(method, method.getNameAsString(), method.getName(), method.getParameters(), method.getBody()));
      case ConstructorDeclaration constructor -> Optional.of(new Unit(
          constructor, typeName, constructor.getName(), constructor.getParameters(),
          Optional.of(constructor.getBody())));
      case CompactConstructorDeclaration constructor -> Optional.of(new Unit(
          constructor, typeName, constructor.getName(), ((RecordDeclaration) type).getParameters(),
          Optional.of(constructor.getBody())));
      default -> Optional.empty();
    };
  }

  private static boolean isInterface(final TypeDeclaration<?> type) {
    return type instanceof ClassOrInterfaceDeclaration declaration && declaration.isInterface();
  }

  private static String signature(final List<Parameter> parameters) {
    return parameters.stream()
        .map(parameter -> erasure(parameter.getType()) + (parameter.isVarArgs() ? "..." : ""))
        .collect(joining(",", "(", ")"));
  }

  /** The type as written, without type arguments and annotations: {@code List<String>} becomes {@code List}. */
  private static String erasure(final Type type) {
    return switch (type) {
      case ArrayType array -> erasure(array.getComponentType()) + "[]";
      case ClassOrInterfaceType classType -> classType.getNameWithScope();
      default -> type.asString();
    };
  }

  private static CognitiveMetrics measure(final String className, final String methodName, final Unit unit) {
    final Range range = unit.declaration().getRange().orElseThrow();
    final CognitiveMetrics metrics = new CognitiveMetrics(className, methodName, range.begin.line);
    metrics.setValue(MetricName.LINE_COUNT, range.end.line - range.begin.line + 1);
    metrics.setValue(MetricName.ARG_COUNT, unit.parameters().size());

    final MethodBodyVisitor visitor = new MethodBodyVisitor(
        unit.parameters().stream().map(Parameter::getNameAsString).collect(toSet()));
    final CyclomaticVisitor cyclomatic = new CyclomaticVisitor();
    final HalsteadVisitor halstead = new HalsteadVisitor();
    final UnderstandabilityVisitor understandability = new UnderstandabilityVisitor(
        unit.declaration() instanceof MethodDeclaration ? Optional.of(unit.name()) : Optional.empty());
    unit.body().ifPresent(body -> {
      body.accept(visitor, null);
      body.accept(cyclomatic, null);
      body.accept(halstead, null);
      body.accept(understandability, null);
    });
    visitor.writeTo(metrics);
    metrics.setCyclomatic(cyclomatic.result());
    metrics.setHalstead(halstead.result());
    metrics.setUnderstandability(understandability.result());
    return metrics;
  }
}
