package net.floriankraemer.cognitive_analysis.domain;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts the decision points of one method body. Each if (an else-if is an if), loop, catch clause,
 * ternary, {@code &&} and {@code ||} counts one. A switch counts one per case entry that has labels,
 * so {@code case A, B ->} is one; the default entry, else and lambdas add nothing by themselves.
 */
final class CyclomaticVisitor extends UnitScopeVisitor {

  private final Map<String, Integer> counts = new LinkedHashMap<>();

  CyclomaticVisitor() {
    List.of("if", "case", "while", "doWhile", "for", "forEach", "catch", "logicalAnd", "logicalOr", "ternary")
        .forEach(kind -> counts.put(kind, 0));
  }

  CyclomaticComplexity result() {
    return CyclomaticComplexity.of(counts);
  }

  private void count(final String kind) {
    counts.merge(kind, 1, Integer::sum);
  }

  @Override
  public void visit(final IfStmt statement, final Void arg) {
    count("if");
    super.visit(statement, arg);
  }

  @Override
  public void visit(final SwitchEntry entry, final Void arg) {
    if (!entry.getLabels().isEmpty() && !entry.isDefault()) {
      count("case");
    }
    super.visit(entry, arg);
  }

  @Override
  public void visit(final WhileStmt statement, final Void arg) {
    count("while");
    super.visit(statement, arg);
  }

  @Override
  public void visit(final DoStmt statement, final Void arg) {
    count("doWhile");
    super.visit(statement, arg);
  }

  @Override
  public void visit(final ForStmt statement, final Void arg) {
    count("for");
    super.visit(statement, arg);
  }

  @Override
  public void visit(final ForEachStmt statement, final Void arg) {
    count("forEach");
    super.visit(statement, arg);
  }

  @Override
  public void visit(final CatchClause clause, final Void arg) {
    count("catch");
    super.visit(clause, arg);
  }

  @Override
  public void visit(final ConditionalExpr expression, final Void arg) {
    count("ternary");
    super.visit(expression, arg);
  }

  @Override
  public void visit(final BinaryExpr expression, final Void arg) {
    switch (expression.getOperator()) {
      case AND -> count("logicalAnd");
      case OR -> count("logicalOr");
      default -> { }
    }
    super.visit(expression, arg);
  }
}
