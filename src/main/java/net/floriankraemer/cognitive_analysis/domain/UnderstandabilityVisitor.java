package net.floriankraemer.cognitive_analysis.domain;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Measures the cognitive complexity of one method body as specified in the SonarSource white paper
 * "Cognitive Complexity" by G. Ann Campbell.
 *
 * <p>Structural increments add 1 plus the current nesting and nest their body: if, ternary, switch
 * (once, not per case), for, for-each, while, do-while and catch. Hybrid increments add 1 and nest
 * their body at the level of the if they belong to: else and else-if. Lambdas nest without an
 * increment. Fundamental increments add 1: each run of like operators in a flattened {@code &&}/
 * {@code ||} sequence, each labeled break or continue, and recursion once per method. Conditions,
 * selectors and loop headers stay at the level of their statement; only bodies nest.
 */
final class UnderstandabilityVisitor extends UnitScopeVisitor {

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
   * @param classSimpleName the simple name of the declaring class, which a recursive call may use as scope
   */
  UnderstandabilityVisitor(final Optional<String> methodName, final String classSimpleName) {
    this.methodName = methodName;
    this.classSimpleName = classSimpleName;
  }

  Understandability result() {
    final int recursion = recursive ? 1 : 0;
    return Understandability.of(
        new Understandability.Increments(structural, hybrid, fundamental + recursion, nestingPoints, recursion));
  }

  private void structural() {
    structural++;
    nestingPoints += nesting;
  }

  private void nested(final Node node) {
    nesting++;
    node.accept(this, null);
    nesting--;
  }

  private void nested(final List<? extends Node> nodes) {
    nodes.forEach(this::nested);
  }

  @Override
  public void visit(final IfStmt statement, final Void arg) {
    structural();
    visitIfChain(statement);
  }

  /** An else-if is part of the chain: it is no new structure, so its body is not nested deeper. */
  private void visitIfChain(final IfStmt statement) {
    statement.getCondition().accept(this, null);
    nested(statement.getThenStmt());
    statement.getElseStmt().ifPresent(otherwise -> {
      hybrid++;
      if (otherwise instanceof IfStmt elseIf) {
        visitIfChain(elseIf);
      } else {
        nested(otherwise);
      }
    });
  }

  @Override
  public void visit(final ConditionalExpr expression, final Void arg) {
    structural();
    expression.getCondition().accept(this, arg);
    nested(expression.getThenExpr());
    nested(expression.getElseExpr());
  }

  @Override
  public void visit(final SwitchStmt statement, final Void arg) {
    structural();
    statement.getSelector().accept(this, arg);
    nested(statement.getEntries());
  }

  @Override
  public void visit(final SwitchExpr expression, final Void arg) {
    structural();
    expression.getSelector().accept(this, arg);
    nested(expression.getEntries());
  }

  @Override
  public void visit(final ForStmt statement, final Void arg) {
    structural();
    statement.getInitialization().forEach(node -> node.accept(this, arg));
    statement.getCompare().ifPresent(node -> node.accept(this, arg));
    statement.getUpdate().forEach(node -> node.accept(this, arg));
    nested(statement.getBody());
  }

  @Override
  public void visit(final ForEachStmt statement, final Void arg) {
    structural();
    statement.getIterable().accept(this, arg);
    nested(statement.getBody());
  }

  @Override
  public void visit(final WhileStmt statement, final Void arg) {
    structural();
    statement.getCondition().accept(this, arg);
    nested(statement.getBody());
  }

  @Override
  public void visit(final DoStmt statement, final Void arg) {
    structural();
    nested(statement.getBody());
    statement.getCondition().accept(this, arg);
  }

  @Override
  public void visit(final CatchClause clause, final Void arg) {
    structural();
    nested(clause.getBody());
  }

  @Override
  public void visit(final LambdaExpr expression, final Void arg) {
    nested(expression.getBody());
  }

  @Override
  public void visit(final BreakStmt statement, final Void arg) {
    if (statement.getLabel().isPresent()) {
      fundamental++;
    }
  }

  @Override
  public void visit(final ContinueStmt statement, final Void arg) {
    if (statement.getLabel().isPresent()) {
      fundamental++;
    }
  }

  /**
   * A call is recursive when it has the method's name (overloads match too) and no scope, {@code this},
   * {@code Own.this} or the class name {@code Own} as scope. {@code super} and any other scope are no recursion.
   */
  @Override
  public void visit(final MethodCallExpr call, final Void arg) {
    if (methodName.filter(call.getNameAsString()::equals).isPresent()
        && call.getScope().map(this::isOwnScope).orElse(true)) {
      recursive = true;
    }
    super.visit(call, arg);
  }

  private boolean isOwnScope(final Expression scope) {
    return switch (scope) {
      case ThisExpr self -> self.getTypeName().map(type -> type.getIdentifier().equals(classSimpleName)).orElse(true);
      case NameExpr name -> name.getNameAsString().equals(classSimpleName);
      default -> false;
    };
  }

  /** Counts a whole sequence once, at its outermost operator; nested operators belong to it. */
  @Override
  public void visit(final BinaryExpr expression, final Void arg) {
    if (isLogical(expression) && !isLogical(parentSkippingParentheses(expression))) {
      fundamental += operatorRuns(expression);
    }
    super.visit(expression, arg);
  }

  /** The number of runs of like operators in the sequence, e.g. 2 for {@code a && b || c}. */
  private static int operatorRuns(final BinaryExpr sequence) {
    final List<Operator> operators = new ArrayList<>();
    flatten(sequence, operators);
    return (int) IntStream.range(0, operators.size())
        .filter(i -> i == 0 || operators.get(i) != operators.get(i - 1))
        .count();
  }

  /** Collects the operators of a sequence in source order, looking through parentheses. */
  private static void flatten(final Expression expression, final List<Operator> operators) {
    final Expression unwrapped = skipParentheses(expression);
    if (unwrapped instanceof BinaryExpr binary && isLogical(binary)) {
      flatten(binary.getLeft(), operators);
      operators.add(binary.getOperator());
      flatten(binary.getRight(), operators);
    }
  }

  private static Expression skipParentheses(final Expression expression) {
    return expression instanceof EnclosedExpr enclosed ? skipParentheses(enclosed.getInner()) : expression;
  }

  private static Node parentSkippingParentheses(final Node node) {
    final Node parent = node.getParentNode().orElseThrow();
    return parent instanceof EnclosedExpr ? parentSkippingParentheses(parent) : parent;
  }

  private static boolean isLogical(final Node node) {
    return node instanceof BinaryExpr binary
        && (binary.getOperator() == Operator.AND || binary.getOperator() == Operator.OR);
  }
}
