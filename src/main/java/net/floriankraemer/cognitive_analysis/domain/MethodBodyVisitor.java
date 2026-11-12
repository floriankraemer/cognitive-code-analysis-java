package net.floriankraemer.cognitive_analysis.domain;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashSet;
import java.util.Set;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/**
 * Counts the metrics of one method body, including its lambdas but not the bodies of anonymous or
 * local classes. Use one instance per body.
 *
 * <ul>
 *   <li>returnCount: return statements.
 *   <li>ifCount: if statements; an else-if continues its chain and does not count.
 *   <li>ifNestingLevel: the deepest if inside another if's branches; an else-if keeps the depth.
 *   <li>elseCount: else and else-if branches.
 *   <li>variableCount: distinct names declared in the body (locals, for and for-each variables,
 *       lambda and catch parameters, pattern variables, try resources) except the parameters and the
 *       unnamed {@code _}, plus {@code this} once when the method reads an instance member.
 *   <li>propertyCallCount: distinct field names accessed with a dot, except static accesses, and
 *       bare names that are neither parameters nor declared in the body. Such a name is read as an
 *       implicit {@code this.name}, like {@code $this->name} in PHP, so it also counts {@code this}.
 * </ul>
 *
 * <p>ponytail: there is no symbol resolution. A name declared anywhere in the method is a local
 * everywhere in it, so a field read before a local of the same name is declared counts as that
 * local. A name that starts with an upper-case letter is taken for a type or a constant: it is no
 * variable or field, and a field accessed on it is static and not counted. Names that only qualify
 * such a name ({@code java.util} in {@code java.util.List}) are package names, and
 * {@code array.length} counts as a property. A symbol solver would remove these guesses at the cost
 * of a classpath.
 */
final class MethodBodyVisitor extends VoidVisitorAdapter<Void> {

  static final String THIS = "this";
  private static final String UNNAMED = "_";
  private static final int MIN_PACKAGE_SEGMENTS = 2;

  private int returnCount;
  private int ifCount;
  private int elseCount;
  private int ifDepth;
  private int maxIfDepth;
  private boolean readsThis;
  private final Set<String> parameters;
  private final Set<String> declaredNames = new HashSet<>();
  private final Set<String> bareNames = new HashSet<>();
  private final Set<String> properties = new HashSet<>();

  /** @param parameters the names of the method parameters, which are no variables */
  MethodBodyVisitor(final Set<String> parameters) {
    this.parameters = parameters;
  }

  /** Call after the body was visited: whether a bare name is a local is known only then. */
  void writeTo(final CognitiveMetrics metrics) {
    final Set<String> variables = new HashSet<>(declaredNames);
    variables.removeAll(parameters);
    final Set<String> implicitFields = new HashSet<>(bareNames);
    implicitFields.removeAll(declaredNames);
    implicitFields.removeAll(parameters);
    final Set<String> allProperties = new HashSet<>(properties);
    allProperties.addAll(implicitFields);
    if (readsThis || !implicitFields.isEmpty()) {
      variables.add(THIS);
    }

    metrics.setValue(MetricName.RETURN_COUNT, returnCount);
    metrics.setValue(MetricName.VARIABLE_COUNT, variables.size());
    metrics.setValue(MetricName.PROPERTY_CALL_COUNT, allProperties.size());
    metrics.setValue(MetricName.IF_COUNT, ifCount);
    metrics.setValue(MetricName.IF_NESTING_LEVEL, maxIfDepth);
    metrics.setValue(MetricName.ELSE_COUNT, elseCount);
  }

  @Override
  public void visit(final ReturnStmt statement, final Void arg) {
    returnCount++;
    super.visit(statement, arg);
  }

  @Override
  public void visit(final IfStmt statement, final Void arg) {
    ifCount++;
    ifDepth++;
    maxIfDepth = Math.max(maxIfDepth, ifDepth);
    visitChain(statement, arg);
    ifDepth--;
  }

  private void visitChain(final IfStmt statement, final Void arg) {
    statement.getCondition().accept(this, arg);
    statement.getThenStmt().accept(this, arg);
    statement.getElseStmt().ifPresent(elseBranch -> {
      elseCount++;
      if (elseBranch instanceof IfStmt elseIf) {
        visitChain(elseIf, arg);
      } else {
        elseBranch.accept(this, arg);
      }
    });
  }

  @Override
  public void visit(final VariableDeclarator declarator, final Void arg) {
    declare(declarator.getNameAsString());
    super.visit(declarator, arg);
  }

  /** Only lambda and catch parameters are visited, the body does not contain the method's own. */
  @Override
  public void visit(final Parameter parameter, final Void arg) {
    declare(parameter.getNameAsString());
    super.visit(parameter, arg);
  }

  @Override
  public void visit(final TypePatternExpr pattern, final Void arg) {
    declare(pattern.getNameAsString());
    super.visit(pattern, arg);
  }

  @Override
  public void visit(final NameExpr name, final Void arg) {
    if (!isTypeName(name.getNameAsString()) && !isPackageQualifier(name)) {
      bareNames.add(name.getNameAsString());
    }
    super.visit(name, arg);
  }

  @Override
  public void visit(final ThisExpr expression, final Void arg) {
    readsThis = true;
    super.visit(expression, arg);
  }

  @Override
  public void visit(final FieldAccessExpr access, final Void arg) {
    final String field = access.getNameAsString();
    if (!isTypeName(field) && !isTypeExpression(access.getScope()) && !isPackageQualifier(access)) {
      properties.add(field);
    }
    super.visit(access, arg);
  }

  /** The arguments belong to the method; an anonymous class body is a unit of its own and skipped. */
  @Override
  public void visit(final ObjectCreationExpr creation, final Void arg) {
    creation.getScope().ifPresent(scope -> scope.accept(this, arg));
    creation.getArguments().forEach(argument -> argument.accept(this, arg));
  }

  @Override
  public void visit(final LocalClassDeclarationStmt statement, final Void arg) {
    // local classes are skipped like anonymous classes
  }

  @Override
  public void visit(final LocalRecordDeclarationStmt statement, final Void arg) {
    // local records are skipped like anonymous classes
  }

  /** The unnamed variable {@code _} names nothing to keep in mind. */
  private void declare(final String name) {
    if (!UNNAMED.equals(name)) {
      declaredNames.add(name);
    }
  }

  private static boolean isTypeName(final String name) {
    return Character.isUpperCase(name.charAt(0));
  }

  private static boolean isTypeExpression(final Expression expression) {
    return (expression instanceof NameExpr name && isTypeName(name.getNameAsString()))
        || (expression instanceof FieldAccessExpr access && isTypeName(access.getNameAsString()));
  }

  /**
   * True for {@code java} and {@code java.util} in {@code java.util.List}: part of a chain of at least
   * two lower-case names that is qualified by an upper-case type name. A single name, as in
   * {@code other.MAX}, is a variable or field, not a package. An expression whose parent is a field
   * access is always its scope.
   */
  private static boolean isPackageQualifier(final Expression expression) {
    Expression chain = expression;
    while (chain.getParentNode().orElseThrow() instanceof FieldAccessExpr access
        && !isTypeName(access.getNameAsString())) {
      chain = access;
    }
    return chain.getParentNode().orElseThrow() instanceof FieldAccessExpr
        && lowerCaseNameSegments(chain) >= MIN_PACKAGE_SEGMENTS;
  }

  /** The number of names in a chain like {@code a.b.c}, or 0 when it holds anything else. */
  private static int lowerCaseNameSegments(final Expression expression) {
    if (expression instanceof NameExpr name) {
      return isTypeName(name.getNameAsString()) ? 0 : 1;
    }
    if (expression instanceof FieldAccessExpr access && !isTypeName(access.getNameAsString())) {
      final int scopeSegments = lowerCaseNameSegments(access.getScope());
      return scopeSegments == 0 ? 0 : scopeSegments + 1;
    }
    return 0;
  }
}
