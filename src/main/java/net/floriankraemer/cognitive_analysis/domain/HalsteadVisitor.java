package net.floriankraemer.cognitive_analysis.domain;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Collects the operators and operands of one method body.
 *
 * <ul>
 *   <li>Operators: binary, assignment and unary operators by symbol, method calls as
 *       {@code call:name}, object creations as {@code new:Type}, {@code ?:} and {@code instanceof}.
 *   <li>Operands: names, accessed field names and literals by their source text, so {@code "1"} and
 *       {@code 1} differ.
 * </ul>
 *
 * <p>ponytail: no symbol resolution, so a type name used as a call scope ({@code Math} in
 * {@code Math.max}) is an operand like a variable.
 */
final class HalsteadVisitor extends UnitScopeVisitor {

  /** Keeps unary {@code -x} apart from binary {@code a - b}; prefix and postfix {@code ++} stay one operator. */
  private static final String UNARY_PREFIX = "u";

  private final List<String> operators = new ArrayList<>();
  private final List<String> operands = new ArrayList<>();

  HalsteadMetrics result() {
    return new HalsteadMetrics(
        new HashSet<>(operators).size(), new HashSet<>(operands).size(), operators.size(), operands.size());
  }

  @Override
  public void visit(final BinaryExpr expression, final Void arg) {
    operators.add(expression.getOperator().asString());
    super.visit(expression, arg);
  }

  @Override
  public void visit(final AssignExpr expression, final Void arg) {
    operators.add(expression.getOperator().asString());
    super.visit(expression, arg);
  }

  @Override
  public void visit(final UnaryExpr expression, final Void arg) {
    operators.add(UNARY_PREFIX + expression.getOperator().asString());
    super.visit(expression, arg);
  }

  @Override
  public void visit(final MethodCallExpr call, final Void arg) {
    operators.add("call:" + call.getNameAsString());
    super.visit(call, arg);
  }

  /** {@code List::size} is a call like {@code list.size()}; its scope is an operand if it is a name. */
  @Override
  public void visit(final MethodReferenceExpr reference, final Void arg) {
    operators.add("call:" + reference.getIdentifier());
    super.visit(reference, arg);
  }

  @Override
  public void visit(final ObjectCreationExpr creation, final Void arg) {
    operators.add("new:" + creation.getType().getNameAsString());
    super.visit(creation, arg);
  }

  @Override
  public void visit(final ConditionalExpr expression, final Void arg) {
    operators.add("?:");
    super.visit(expression, arg);
  }

  @Override
  public void visit(final InstanceOfExpr expression, final Void arg) {
    operators.add("instanceof");
    super.visit(expression, arg);
  }

  @Override
  public void visit(final NameExpr name, final Void arg) {
    operands.add(name.getNameAsString());
  }

  @Override
  public void visit(final FieldAccessExpr access, final Void arg) {
    operands.add(access.getNameAsString());
    super.visit(access, arg);
  }

  @Override
  public void visit(final StringLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final CharLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final IntegerLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final LongLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final DoubleLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final BooleanLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final NullLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  @Override
  public void visit(final TextBlockLiteralExpr literal, final Void arg) {
    literal(literal);
  }

  private void literal(final LiteralExpr literal) {
    operands.add(literal.toString());
  }
}
