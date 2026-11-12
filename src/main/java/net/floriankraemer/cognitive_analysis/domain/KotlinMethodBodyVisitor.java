package net.floriankraemer.cognitive_analysis.domain;

import java.util.HashSet;
import java.util.Set;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFunctionLiteral;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.KtThisExpression;

/**
 * Counts the metrics of one Kotlin function body with the rules of {@link MethodBodyVisitor}.
 *
 * <ul>
 *   <li>returnCount: return expressions, also labeled ones like {@code return@forEach}; the value of
 *       an expression body is no return.
 *   <li>ifCount, ifNestingLevel, elseCount: if statements and if expressions, as in Java.
 *   <li>variableCount: distinct names declared with {@code val} or {@code var}, destructuring entries,
 *       lambda, local function, catch and for-loop parameters, and the implicit lambda parameter
 *       {@code it} when it is used, except the function parameters and {@code _}; plus {@code this}
 *       once when the function reads an instance member.
 *   <li>propertyCallCount: distinct property names accessed with {@code .} or {@code ?.}, except on
 *       an upper-case name, and bare names that are neither parameters nor declared in the body.
 * </ul>
 */
final class KotlinMethodBodyVisitor extends KotlinUnitScopeVisitor {

  private static final String UNNAMED = "_";
  private static final String IMPLICIT_PARAMETER = "it";

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

  /** @param parameters the names of the function parameters, which are no variables */
  KotlinMethodBodyVisitor(final Set<String> parameters) {
    this.parameters = parameters;
  }

  /** Call after the body was visited: whether a bare name is a local is known only then. */
  void writeTo(final CognitiveMetrics metrics) {
    final Set<String> variables = new HashSet<>(declaredNames);
    variables.removeAll(parameters);
    final Set<String> implicitProperties = new HashSet<>(bareNames);
    implicitProperties.removeAll(declaredNames);
    implicitProperties.removeAll(parameters);
    final Set<String> allProperties = new HashSet<>(properties);
    allProperties.addAll(implicitProperties);
    if (readsThis || !implicitProperties.isEmpty()) {
      variables.add(MethodBodyVisitor.THIS);
    }

    metrics.setValue(MetricName.RETURN_COUNT, returnCount);
    metrics.setValue(MetricName.VARIABLE_COUNT, variables.size());
    metrics.setValue(MetricName.PROPERTY_CALL_COUNT, allProperties.size());
    metrics.setValue(MetricName.IF_COUNT, ifCount);
    metrics.setValue(MetricName.IF_NESTING_LEVEL, maxIfDepth);
    metrics.setValue(MetricName.ELSE_COUNT, elseCount);
  }

  @Override
  public void visitReturnExpression(final KtReturnExpression expression) {
    returnCount++;
    super.visitReturnExpression(expression);
  }

  @Override
  public void visitIfExpression(final KtIfExpression expression) {
    ifCount++;
    ifDepth++;
    maxIfDepth = Math.max(maxIfDepth, ifDepth);
    visitChain(expression);
    ifDepth--;
  }

  private void visitChain(final KtIfExpression expression) {
    acceptIfPresent(expression.getCondition());
    acceptIfPresent(expression.getThen());
    final KtExpression elseBranch = expression.getElse();
    if (elseBranch != null) {
      elseCount++;
      if (elseBranch instanceof KtIfExpression elseIf) {
        visitChain(elseIf);
      } else {
        elseBranch.accept(this);
      }
    }
  }

  private void acceptIfPresent(final KtExpression expression) {
    if (expression != null) {
      expression.accept(this);
    }
  }

  @Override
  public void visitProperty(final KtProperty property) {
    declare(property.getName());
    super.visitProperty(property);
  }

  /** Lambda, local function, catch and for-loop parameters: the body does not contain the function's own. */
  @Override
  public void visitParameter(final KtParameter parameter) {
    declare(parameter.getName());
    super.visitParameter(parameter);
  }

  /** Its children are the name and the type, which name nothing to count. */
  @Override
  public void visitDestructuringDeclarationEntry(final KtDestructuringDeclarationEntry entry) {
    declare(entry.getName());
  }

  /** Its children are the keyword and the label, which name nothing to count. */
  @Override
  public void visitThisExpression(final KtThisExpression expression) {
    readsThis = true;
  }

  @Override
  public void visitSimpleNameExpression(final KtSimpleNameExpression expression) {
    if (expression instanceof KtNameReferenceExpression name && isValueName(name)) {
      countName(name);
    }
    super.visitSimpleNameExpression(expression);
  }

  private void countName(final KtNameReferenceExpression name) {
    final String identifier = name.getReferencedName();
    if (isTypeName(identifier) || isPackageQualifier(name)) {
      return;
    }
    if (name.getParent() instanceof KtQualifiedExpression qualified && qualified.getSelectorExpression() == name) {
      if (!isTypeExpression(qualified.getReceiverExpression())) {
        properties.add(identifier);
      }
    } else if (isImplicitLambdaParameter(name)) {
      declare(identifier);
    } else {
      bareNames.add(identifier);
    }
  }

  private static boolean isImplicitLambdaParameter(final KtNameReferenceExpression name) {
    final KtFunctionLiteral lambda = PsiTreeUtil.getParentOfType(name, KtFunctionLiteral.class);
    return IMPLICIT_PARAMETER.equals(name.getReferencedName()) && lambda != null && !lambda.hasParameterSpecification();
  }

  /** A destructured lambda parameter has no name of its own, and {@code _} names nothing to keep in mind. */
  private void declare(final String name) {
    if (name != null && !UNNAMED.equals(name)) {
      declaredNames.add(name);
    }
  }
}
