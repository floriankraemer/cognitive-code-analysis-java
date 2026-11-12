package net.floriankraemer.cognitive_analysis.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression;
import org.jetbrains.kotlin.psi.KtConstantExpression;
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtIsExpression;
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtUnaryExpression;
import org.jetbrains.kotlin.psi.KtWhenConditionInRange;
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern;

/**
 * Collects the operators and operands of one Kotlin function body with the rules of
 * {@link HalsteadVisitor}.
 *
 * <ul>
 *   <li>Operators: binary operators by symbol, including assignments, elvis {@code ?:}, ranges
 *       {@code ..} and {@code in}; unary operators by symbol, including {@code !!}; {@code is}; the
 *       safe call {@code ?.}; calls as {@code call:name}, infix calls and callable references
 *       included; calls of an upper-case name as {@code new:Type}, as they create an object.
 *   <li>Operands: names and literals by their source text. A string template without
 *       interpolation is one operand like a Java string literal; otherwise each literal part is one.
 * </ul>
 */
final class KotlinHalsteadVisitor extends KotlinUnitScopeVisitor {

  /** Keeps unary {@code -x} apart from binary {@code a - b}; prefix and postfix {@code ++} stay one operator. */
  private static final String UNARY_PREFIX = "u";
  private static final String CALL = "call:";

  private final List<String> operators = new ArrayList<>();
  private final List<String> operands = new ArrayList<>();

  HalsteadMetrics result() {
    return new HalsteadMetrics(
        new HashSet<>(operators).size(), new HashSet<>(operands).size(), operators.size(), operands.size());
  }

  /** An infix call like {@code a to b} is a call; every other operator is named by its symbol. */
  @Override
  public void visitBinaryExpression(final KtBinaryExpression expression) {
    final String operator = expression.getOperationReference().getReferencedName();
    operators.add(expression.getOperationToken() == KtTokens.IDENTIFIER ? CALL + operator : operator);
    super.visitBinaryExpression(expression);
  }

  @Override
  public void visitUnaryExpression(final KtUnaryExpression expression) {
    operators.add(UNARY_PREFIX + expression.getOperationReference().getReferencedName());
    super.visitUnaryExpression(expression);
  }

  @Override
  public void visitIsExpression(final KtIsExpression expression) {
    operators.add(expression.isNegated() ? "!is" : "is");
    super.visitIsExpression(expression);
  }

  /** Its only child is the type, which is no operand. */
  @Override
  public void visitWhenConditionIsPattern(final KtWhenConditionIsPattern condition) {
    operators.add(condition.isNegated() ? "!is" : "is");
  }

  @Override
  public void visitWhenConditionInRange(final KtWhenConditionInRange condition) {
    operators.add(condition.isNegated() ? "!in" : "in");
    super.visitWhenConditionInRange(condition);
  }

  @Override
  public void visitSafeQualifiedExpression(final KtSafeQualifiedExpression expression) {
    operators.add("?.");
    super.visitSafeQualifiedExpression(expression);
  }

  @Override
  public void visitCallExpression(final KtCallExpression call) {
    if (call.getCalleeExpression() instanceof KtNameReferenceExpression callee) {
      final String name = callee.getReferencedName();
      operators.add(isTypeName(name) ? "new:" + name : CALL + name);
    } else {
      operators.add(CALL + "invoke");
    }
    super.visitCallExpression(call);
  }

  /** {@code List::size} is a call like {@code list.size()}; its receiver is an operand if it is a name. */
  @Override
  public void visitCallableReferenceExpression(final KtCallableReferenceExpression reference) {
    operators.add(CALL + reference.getCallableReference().getReferencedName());
    super.visitCallableReferenceExpression(reference);
  }

  @Override
  public void visitSimpleNameExpression(final KtSimpleNameExpression expression) {
    if (expression instanceof KtNameReferenceExpression name && isValueName(name)) {
      operands.add(name.getReferencedName());
    }
  }

  @Override
  public void visitConstantExpression(final KtConstantExpression expression) {
    operands.add(expression.getText());
  }

  @Override
  public void visitStringTemplateExpression(final KtStringTemplateExpression expression) {
    if (expression.hasInterpolation()) {
      super.visitStringTemplateExpression(expression);
    } else {
      operands.add(expression.getText());
    }
  }

  @Override
  public void visitLiteralStringTemplateEntry(final KtLiteralStringTemplateEntry entry) {
    operands.add(entry.getText());
  }

  @Override
  public void visitEscapeStringTemplateEntry(final KtEscapeStringTemplateEntry entry) {
    operands.add(entry.getText());
  }
}
