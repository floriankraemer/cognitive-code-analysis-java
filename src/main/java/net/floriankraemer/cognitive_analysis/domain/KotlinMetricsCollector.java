package net.floriankraemer.cognitive_analysis.domain;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.jetbrains.kotlin.cli.FrontendConfigurationKeysKt;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunctionType;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtNullableType;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
import org.jetbrains.kotlin.psi.KtTypeElement;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtUserType;

/**
 * Parses Kotlin source code and measures the cognitive metrics of every function and secondary
 * constructor, with the same metrics as {@link CognitiveMetricsCollector} measures for Java.
 *
 * <p>The parser is the PSI of the Kotlin compiler, without type resolution. Its environment is
 * created once per process, on the first Kotlin file. The collector is stateless and thread-safe.
 *
 * <p>Units are the functions and secondary constructors of classes, interfaces, objects, companion
 * objects and enum classes, and the top-level functions. A class is keyed by its fully qualified
 * name with nested classes separated by dots; a companion object without a name is
 * {@code Companion}. Top-level functions belong to the JVM facade class of the file,
 * {@code pkg.FileNameKt} or the name of {@code @file:JvmName}. Interface functions count only with
 * a body; abstract and external class functions count with empty metrics. Secondary constructors
 * are named after their class; {@code init} blocks, property accessors, enum entry bodies, anonymous
 * objects and local classes are skipped; lambdas and local functions belong to their function.
 * Overloads get their parameter types appended like in Java, an extension receiver as the first
 * type. A {@value CognitiveMetricsCollector#IGNORE_TAG} comment skips a declaration.
 */
public class KotlinMetricsCollector {

  /**
   * @param fileName the name of the file without directories; it names the facade class of the
   *     top-level functions
   * @return the metrics per function key, in source order
   * @throws SourceParseException when the code is not valid Kotlin
   */
  public Map<String, CognitiveMetrics> collect(final String fileName, final String code)
      throws SourceParseException {
    final String text = StringUtil.convertLineSeparators(code);
    final KtFile file = new KtPsiFactory(Environment.PROJECT, false).createFile(fileName, text);
    final PsiErrorElement error = PsiTreeUtil.findChildOfType(file, PsiErrorElement.class);
    if (error != null) {
      final int offset = error.getTextRange().getStartOffset();
      final int line = StringUtil.offsetToLineNumber(text, offset);
      final int column = offset - StringUtil.lineColToOffset(text, line, 0) + 1;
      throw new SourceParseException(
          "(line " + (line + 1) + ",col " + column + ") " + error.getErrorDescription());
    }
    return new FileScan(file, text).scan();
  }

  /** The compiler environment, created on first use and kept for the lifetime of the process. */
  private static final class Environment {

    static final Project PROJECT = create();

    private static Project create() {
      final CompilerConfiguration configuration = new CompilerConfiguration();
      configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.Companion.getNONE());
      configuration.put(CommonConfigurationKeys.MODULE_NAME, "cognitive-analysis");
      FrontendConfigurationKeysKt.setExtensionsStorage(configuration, new CompilerPluginRegistrar.ExtensionStorage());
      return KotlinCoreEnvironment.createForProduction(
          Disposer.newDisposable("cognitive-analysis"), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
          .getProject();
    }
  }

  /** The state of collecting one file. */
  private static final class FileScan {

    private final KtFile file;
    private final String text;
    private final List<PsiComment> ignoreComments;
    private final Map<String, CognitiveMetrics> metrics = new LinkedHashMap<>();

    FileScan(final KtFile file, final String text) {
      this.file = file;
      this.text = text;
      this.ignoreComments = ignoreComments(file);
    }

    /** Walks the leaves too: line and block comments are leaves, which {@code PsiTreeUtil} searches skip. */
    private static List<PsiComment> ignoreComments(final KtFile file) {
      final List<PsiComment> comments = new ArrayList<>();
      file.accept(new PsiRecursiveElementWalkingVisitor() {
        @Override
        public void visitElement(final PsiElement element) {
          if (element instanceof PsiComment comment && comment.getText().contains(CognitiveMetricsCollector.IGNORE_TAG)) {
            comments.add(comment);
          }
          super.visitElement(element);
        }
      });
      return comments;
    }

    Map<String, CognitiveMetrics> scan() {
      final String packageName = file.getPackageFqName().asString();
      final String facadeClass = JvmFileClassUtil.getFileClassInfoNoResolve(file).getFacadeClassFqName().asString();
      scanMembers(file.getDeclarations(), facadeClass, packageName.isEmpty() ? "" : packageName + ".", false);
      return metrics;
    }

    private void scanType(final KtClassOrObject type, final String className) {
      if (!isIgnored(type)) {
        scanMembers(type.getDeclarations(), className, className + ".",
            type instanceof KtClass declaration && declaration.isInterface());
      }
    }

    /**
     * @param className the class the functions belong to
     * @param nestedPrefix the prefix of the names of nested classes
     */
    private void scanMembers(
        final List<KtDeclaration> members, final String className, final String nestedPrefix,
        final boolean isInterface) {
      final String classSimpleName = className.substring(className.lastIndexOf('.') + 1);
      final Map<String, Long> nameCounts = members.stream()
          .map(member -> unitOf(member, classSimpleName, isInterface))
          .flatMap(Optional::stream)
          .collect(groupingBy(Unit::name, counting()));
      for (final KtDeclaration member : members) {
        if (member instanceof KtClassOrObject nested && !(member instanceof KtEnumEntry)) {
          scanType(nested, nestedPrefix + simpleName(nested));
          continue;
        }
        final Optional<Unit> unit = unitOf(member, classSimpleName, isInterface);
        if (unit.isPresent() && !isIgnored(member)) {
          final Unit found = unit.get();
          final String methodName = nameCounts.get(found.name()) > 1 ? found.name() + signature(found) : found.name();
          metrics.putIfAbsent(className + "::" + methodName, measure(className, methodName, found));
        }
      }
    }

    /**
     * A declaration is ignored when the tag is in a comment that the parser binds to it, such as its KDoc, a
     * comment between its annotations or a comment trailing it on its last line, or in an unbound comment
     * between the previous sibling and the declaration.
     */
    private boolean isIgnored(final KtDeclaration declaration) {
      final List<PsiComment> preceding = precedingComments(declaration);
      return ignoreComments.stream().anyMatch(comment -> comment.getParent() == declaration
          || comment.getParent() == declaration.getModifierList()
          || preceding.contains(comment));
    }

    /**
     * The comments between the previous sibling and the declaration, skipping empty siblings such as a missing
     * package directive. A comment on the line the previous sibling ends on trails that sibling.
     */
    private List<PsiComment> precedingComments(final KtDeclaration declaration) {
      final List<PsiComment> comments = new ArrayList<>();
      PsiElement sibling = declaration.getPrevSibling();
      while (sibling instanceof PsiWhiteSpace || sibling instanceof PsiComment
          || sibling != null && sibling.getTextLength() == 0) {
        if (sibling instanceof PsiComment comment) {
          comments.add(comment);
        }
        sibling = sibling.getPrevSibling();
      }
      if (sibling != null) {
        final int previousLine = lineOf(sibling.getTextRange().getEndOffset());
        comments.removeIf(comment -> lineOf(comment.getTextRange().getStartOffset()) <= previousLine);
      }
      return comments;
    }

    /** @return the 1-based line of the offset */
    private int lineOf(final int offset) {
      return StringUtil.offsetToLineNumber(text, offset) + 1;
    }

    private CognitiveMetrics measure(final String className, final String methodName, final Unit unit) {
      final List<PsiElement> code = codeChildren(unit.declaration());
      final int firstLine = lineOf(code.getFirst().getTextRange().getStartOffset());
      final int lastLine = lineOf(code.getLast().getTextRange().getEndOffset());
      final CognitiveMetrics result = new CognitiveMetrics(className, methodName, firstLine);
      result.setValue(MetricName.LINE_COUNT, lastLine - firstLine + 1);
      result.setValue(MetricName.ARG_COUNT, unit.declaration().getValueParameters().size());

      final Set<String> parameters = unit.declaration().getValueParameters().stream()
          .map(KtParameter::getName)
          .collect(Collectors.toSet());
      final KotlinMethodBodyVisitor visitor = new KotlinMethodBodyVisitor(parameters);
      final KotlinCyclomaticVisitor cyclomatic = new KotlinCyclomaticVisitor();
      final KotlinHalsteadVisitor halstead = new KotlinHalsteadVisitor();
      final KotlinUnderstandabilityVisitor understandability = new KotlinUnderstandabilityVisitor(
          unit.declaration() instanceof KtNamedFunction ? Optional.of(unit.name()) : Optional.empty(),
          className.substring(className.lastIndexOf('.') + 1));
      unit.body().ifPresent(body -> {
        body.accept(visitor);
        body.accept(cyclomatic);
        body.accept(halstead);
        body.accept(understandability);
      });
      visitor.writeTo(result);
      result.setCyclomatic(cyclomatic.result());
      result.setHalstead(halstead.result());
      result.setUnderstandability(understandability.result());
      return result;
    }

    /** The children that are code: the lines start at the annotations, not at the KDoc. */
    private static List<PsiElement> codeChildren(final PsiElement declaration) {
      final List<PsiElement> children = new ArrayList<>();
      for (PsiElement child = declaration.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (!(child instanceof PsiWhiteSpace) && !(child instanceof PsiComment)) {
          children.add(child);
        }
      }
      return children;
    }
  }

  /** A function or secondary constructor with the name it is reported under. */
  private record Unit(KtCallableDeclaration declaration, String name, Optional<KtExpression> body) {}

  private static Optional<Unit> unitOf(
      final KtDeclaration member, final String classSimpleName, final boolean isInterface) {
    return switch (member) {
      case KtNamedFunction function when function.hasBody() || !isInterface -> Optional.of(
          new Unit(function, function.getName(), Optional.ofNullable(function.getBodyExpression())));
      case KtSecondaryConstructor constructor -> Optional.of(
          new Unit(constructor, classSimpleName, Optional.ofNullable(constructor.getBodyExpression())));
      default -> Optional.empty();
    };
  }

  private static String simpleName(final KtClassOrObject type) {
    return type instanceof KtObjectDeclaration object && object.isCompanion() && object.getNameIdentifier() == null
        ? "Companion"
        : type.getName();
  }

  /** The receiver of an extension function, then the parameter types, as in the JVM signature. */
  private static String signature(final Unit unit) {
    final Stream<String> receiver = Stream.ofNullable(unit.declaration().getReceiverTypeReference())
        .map(KotlinMetricsCollector::erasure);
    final Stream<String> parameters = unit.declaration().getValueParameters().stream()
        .map(parameter -> erasure(parameter.getTypeReference()) + (parameter.isVarArg() ? "..." : ""));
    return Stream.concat(receiver, parameters).collect(joining(",", "(", ")"));
  }

  /**
   * The type as written, without type arguments, nullability and annotations: {@code List<String>?}
   * becomes {@code List}, a function type with n parameters {@code Function<n>} like on the JVM.
   */
  private static String erasure(final KtTypeReference reference) {
    return erasure(reference.getTypeElement());
  }

  private static String erasure(final KtTypeElement type) {
    return switch (type) {
      case KtNullableType nullable -> erasure(nullable.getInnerType());
      case KtUserType user -> (user.getQualifier() == null ? "" : erasure(user.getQualifier()) + ".")
          + user.getReferencedName();
      case KtFunctionType function -> "Function"
          + (function.getParameters().size() + (function.getReceiverTypeReference() == null ? 0 : 1));
      default -> type.getText();
    };
  }
}
