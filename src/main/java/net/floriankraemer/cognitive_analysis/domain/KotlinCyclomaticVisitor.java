package net.floriankraemer.cognitive_analysis.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBinaryExpression;
import org.jetbrains.kotlin.psi.KtCatchClause;
import org.jetbrains.kotlin.psi.KtDoWhileExpression;
import org.jetbrains.kotlin.psi.KtForExpression;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhileExpression;

/**
 * Counts the decision points of one Kotlin function body with the rules of {@link CyclomaticVisitor}.
 * Each if (an else-if is an if), loop, catch clause, {@code &&}, {@code ||} and elvis {@code ?:}
 * counts one. A {@code when} counts one per branch that has conditions, so {@code 1, 2 ->} is one;
 * the else branch adds nothing. A Kotlin {@code for} loop is a for-each loop.
 */
final class KotlinCyclomaticVisitor extends KotlinUnitScopeVisitor {

  private static final Map<IElementType, String> OPERATOR_KINDS =
      Map.of(KtTokens.ANDAND, "logicalAnd", KtTokens.OROR, "logicalOr", KtTokens.ELVIS, "elvis");

  private final Map<String, Integer> counts = new LinkedHashMap<>();

  KotlinCyclomaticVisitor() {
    List.of("if", "case", "while", "doWhile", "forEach", "catch", "logicalAnd", "logicalOr", "elvis")
        .forEach(kind -> counts.put(kind, 0));
  }

  CyclomaticComplexity result() {
    return CyclomaticComplexity.of(counts);
  }

  private void count(final String kind) {
    counts.merge(kind, 1, Integer::sum);
  }

  @Override
  public void visitIfExpression(final KtIfExpression expression) {
    count("if");
    super.visitIfExpression(expression);
  }

  @Override
  public void visitWhenEntry(final KtWhenEntry entry) {
    if (!entry.isElse()) {
      count("case");
    }
    super.visitWhenEntry(entry);
  }

  @Override
  public void visitWhileExpression(final KtWhileExpression expression) {
    count("while");
    super.visitWhileExpression(expression);
  }

  @Override
  public void visitDoWhileExpression(final KtDoWhileExpression expression) {
    count("doWhile");
    super.visitDoWhileExpression(expression);
  }

  @Override
  public void visitForExpression(final KtForExpression expression) {
    count("forEach");
    super.visitForExpression(expression);
  }

  @Override
  public void visitCatchSection(final KtCatchClause clause) {
    count("catch");
    super.visitCatchSection(clause);
  }

  @Override
  public void visitBinaryExpression(final KtBinaryExpression expression) {
    final String kind = OPERATOR_KINDS.get(expression.getOperationToken());
    if (kind != null) {
      count(kind);
    }
    super.visitBinaryExpression(expression);
  }
}
