package net.floriankraemer.cognitive_analysis.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtBreakExpression;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtCatchClause;
import org.jetbrains.kotlin.psi.KtContinueExpression;
import org.jetbrains.kotlin.psi.KtDoWhileExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtForExpression;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtLambdaExpression;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtThisExpression;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.psi.KtWhileExpression;

/**
 * Measures the cognitive complexity of one Kotlin function body with the rules of
 * {@link UnderstandabilityVisitor}.
 *
 * <p>Structural increments: if, {@code when} (once, not per branch, with or without subject), for,
 * while, do-while and catch. Hybrid increments: else and else-if. Lambdas and local functions nest
 * without an increment. Fundamental increments: each run of like operators in a flattened
 * {@code &&}/{@code ||} sequence, each labeled break or continue, and recursion once per function.
 * The elvis operator {@code ?:}, safe calls and labeled returns add nothing.
 */
final class KotlinUnderstandabilityVisitor extends KotlinUnitScopeVisitor {

  private final Optional<String> methodName;
  private final String classSimpleName;
  private int nesting;
  private int structural;
  private int hybrid;
  private int fundamental;
  private int nestingPoints;
  private boolean recursive;

  /**
   * @param methodName the name a recursive call has; empty for constructors
   * @param classSimpleName the simple name of the declaring class, which a recursive call may use as receiver
   */
  KotlinUnderstandabilityVisitor(final Optional<String> methodName, final String classSimpleName) {
    this.methodName = methodName;
    this.classSimpleName = classSimpleName;
  }

  Understandability result() {
    final int recursion = recursive ? 1 : 0;
    return Understandability.of(structural, hybrid, fundamental + recursion, nestingPoints, recursion);
  }

  private void structural() {
    structural++;
    nestingPoints += nesting;
  }

  private void nested(final PsiElement element) {
    nesting++;
    accept(element);
    nesting--;
  }

  private void accept(final PsiElement element) {
    if (element != null) {
      element.accept(this);
    }
  }

  @Override
  public void visitIfExpression(final KtIfExpression expression) {
    structural();
    visitIfChain(expression);
  }

  /** An else-if is part of the chain: it is no new structure, so its body is not nested deeper. */
  private void visitIfChain(final KtIfExpression expression) {
    accept(expression.getCondition());
    nested(expression.getThen());
    final KtExpression otherwise = expression.getElse();
    if (otherwise != null) {
      hybrid++;
      if (otherwise instanceof KtIfExpression elseIf) {
        visitIfChain(elseIf);
      } else {
        nested(otherwise);
      }
    }
  }

  @Override
  public void visitWhenExpression(final KtWhenExpression expression) {
    structural();
    accept(expression.getSubjectVariable() != null ? expression.getSubjectVariable() : expression.getSubjectExpression());
    expression.getEntries().forEach(this::nested);
  }

  @Override
  public void visitForExpression(final KtForExpression expression) {
    structural();
    accept(expression.getLoopRange());
    nested(expression.getBody());
  }

  @Override
  public void visitWhileExpression(final KtWhileExpression expression) {
    structural();
    accept(expression.getCondition());
    nested(expression.getBody());
  }

  @Override
  public void visitDoWhileExpression(final KtDoWhileExpression expression) {
    structural();
    nested(expression.getBody());
    accept(expression.getCondition());
  }

  @Override
  public void visitCatchSection(final KtCatchClause clause) {
    structural();
    nested(clause.getCatchBody());
  }

  @Override
  public void visitLambdaExpression(final KtLambdaExpression expression) {
    nested(expression.getBodyExpression());
  }

  /** A local function: the unit itself is visited from its body, so every function met here is local. */
  @Override
  public void visitNamedFunction(final KtNamedFunction function) {
    nested(function.getBodyExpression());
  }

  @Override
  public void visitBreakExpression(final KtBreakExpression expression) {
    if (expression.getTargetLabel() != null) {
      fundamental++;
    }
  }

  @Override
  public void visitContinueExpression(final KtContinueExpression expression) {
    if (expression.getTargetLabel() != null) {
      fundamental++;
    }
  }

  /**
   * A call is recursive when it has the function's name (overloads match too) and no receiver,
   * {@code this}, {@code this@Own} or the class name {@code Own} as receiver.
   */
  @Override
  public void visitCallExpression(final KtCallExpression call) {
    if (call.getCalleeExpression() instanceof KtNameReferenceExpression callee
        && methodName.filter(callee.getReferencedName()::equals).isPresent()
        && receiverOf(call).map(this::isOwnReceiver).orElse(true)) {
      recursive = true;
    }
    super.visitCallExpression(call);
  }

  private static Optional<KtExpression> receiverOf(final KtCallExpression call) {
    return call.getParent() instanceof KtQualifiedExpression qualified && qualified.getSelectorExpression() == call
        ? Optional.of(qualified.getReceiverExpression())
        : Optional.empty();
  }

  private boolean isOwnReceiver(final KtExpression receiver) {
    return switch (receiver) {
      case KtThisExpression self -> self.getLabelName() == null || self.getLabelName().equals(classSimpleName);
      case KtNameReferenceExpression name -> name.getReferencedName().equals(classSimpleName);
      default -> false;
    };
  }

  /** Counts a whole sequence once, at its outermost operator; nested operators belong to it. */
  @Override
  public void visitBinaryExpression(final KtBinaryExpression expression) {
    if (isLogical(expression) && !isLogical(parentSkippingParentheses(expression))) {
      final List<IElementType> operators = new ArrayList<>();
      flatten(expression, operators);
      for (int i = 0; i < operators.size(); i++) {
        if (i == 0 || operators.get(i) != operators.get(i - 1)) {
          fundamental++;
        }
      }
    }
    super.visitBinaryExpression(expression);
  }

  /** Collects the operators of a sequence in source order, looking through parentheses. */
  private static void flatten(final KtExpression expression, final List<IElementType> operators) {
    final KtExpression unwrapped = skipParentheses(expression);
    if (unwrapped instanceof KtBinaryExpression binary && isLogical(binary)) {
      flatten(binary.getLeft(), operators);
      operators.add(binary.getOperationToken());
      flatten(binary.getRight(), operators);
    }
  }

  private static KtExpression skipParentheses(final KtExpression expression) {
    return expression instanceof KtParenthesizedExpression parenthesized
        ? skipParentheses(parenthesized.getExpression())
        : expression;
  }

  private static PsiElement parentSkippingParentheses(final PsiElement element) {
    final PsiElement parent = element.getParent();
    return parent instanceof KtParenthesizedExpression ? parentSkippingParentheses(parent) : parent;
  }

  private static boolean isLogical(final PsiElement element) {
    return element instanceof KtBinaryExpression binary
        && (binary.getOperationToken() == KtTokens.ANDAND || binary.getOperationToken() == KtTokens.OROR);
  }
}
