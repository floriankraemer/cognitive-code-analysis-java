package net.floriankraemer.cognitive_analysis.domain;

import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * Walks one method body including its lambdas but not the bodies of anonymous or local classes,
 * which are units of their own and skipped. Use one instance per body.
 */
abstract class UnitScopeVisitor extends VoidVisitorAdapter<Void> {

  /** Subclasses count the creation itself, then call this to visit what belongs to the method. */
  @Override
  public void visit(final ObjectCreationExpr creation, final Void arg) {
    creation.getScope().ifPresent(scope -> scope.accept(this, arg));
    creation.getArguments().forEach(argument -> argument.accept(this, arg));
  }

  @Override
  public void visit(final LocalClassDeclarationStmt statement, final Void arg) {
    // skipped
  }

  @Override
  public void visit(final LocalRecordDeclarationStmt statement, final Void arg) {
    // skipped
  }
}
