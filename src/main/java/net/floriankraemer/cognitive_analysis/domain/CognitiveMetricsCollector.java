package net.floriankraemer.cognitive_analysis.domain;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * A class to collect cognitive metrics from Java code.
 */
@Component
public class CognitiveMetricsCollector {

  private Map<String, CognitiveMetrics> methodMetrics = new HashMap<>();

  /**
   * Analyze the given Java code to calculate cognitive metrics.
   *
   * @param code The Java code to analyze.
   */
  public void analyze(final String code) {
    methodMetrics = new HashMap<>();

    CompilationUnit compilationUnit = StaticJavaParser.parse(code);
    compilationUnit.accept(new ClassVisitor(), null);
  }

  /**
   * Get the method metrics.
   *
   * @return A map of method names to their cognitive metrics.
   */
  public Map<String, CognitiveMetrics> getMethodMetrics() {
    return methodMetrics;
  }

  private static class StatementVisitor extends VoidVisitorAdapter<Void> {
    private final CognitiveMetrics metrics;
    private final int currentNestingLevel;  // To track the depth of nested structures

    public StatementVisitor(CognitiveMetrics metrics, int nestingLevel) {
      this.metrics = metrics;
      this.currentNestingLevel = nestingLevel;
    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
      // Increment the if statement count and track nesting level
      metrics.incrementIfCount();
      int nestedIfLevel = currentNestingLevel + 1;
      metrics.setIfNestingLevel(Math.max(metrics.getIfNestingLevel(), nestedIfLevel));

      // Visit the 'then' statement only, without recursively re-visiting the entire structure
      ifStmt.getThenStmt().accept(new StatementVisitor(metrics, nestedIfLevel), null);

      // If there is an 'else' statement, increment else count and visit it
      if (ifStmt.getElseStmt().isPresent()) {
        metrics.incrementElseCount();
        ifStmt.getElseStmt().get().accept(new StatementVisitor(metrics, nestedIfLevel), null);
      }
    }

    @Override
    public void visit(TryStmt tryStmt, Void arg) {
      super.visit(tryStmt, arg);

      // Track the nesting level for try-catch blocks
      int nestedTryLevel = currentNestingLevel + 1;
      metrics.setTryCatchNestingLevel(Math.max(metrics.getTryCatchNestingLevel(), nestedTryLevel));

      // Visit the try block and catch clauses with increased nesting level
      tryStmt.getTryBlock().accept(new StatementVisitor(metrics, nestedTryLevel), null);
      tryStmt.getCatchClauses().forEach(
          catchClause -> catchClause.accept(new StatementVisitor(metrics, nestedTryLevel), null));
    }

    @Override
    public void visit(ForStmt forStmt, Void arg) {
      super.visit(forStmt, arg);
      metrics.incrementLoopCount();
    }

    @Override
    public void visit(WhileStmt whileStmt, Void arg) {
      super.visit(whileStmt, arg);
      metrics.incrementLoopCount();
    }

    @Override
    public void visit(DoStmt doStmt, Void arg) {
      super.visit(doStmt, arg);
      metrics.incrementLoopCount();
    }

    @Override
    public void visit(SwitchStmt switchStmt, Void arg) {
      super.visit(switchStmt, arg);
      metrics.incrementSwitchCount();
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Void arg) {
      super.visit(methodCallExpr, arg);
      metrics.incrementMethodCallCount();
    }

    @Override
    public void visit(ReturnStmt returnStmt, Void arg) {
      super.visit(returnStmt, arg);
      metrics.incrementReturnCount();
    }

    @Override
    public void visit(VariableDeclarator variableDeclarator, Void arg) {
      super.visit(variableDeclarator, arg);
      metrics.incrementVariableCount();
    }

    @Override
    public void visit(FieldDeclaration fieldDeclaration, Void arg) {
      super.visit(fieldDeclaration, arg);
      metrics.incrementFieldAccessCount();
    }
  }

  final private class ClassVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
      super.visit(declaration, arg);
      String className = declaration.getNameAsString();

      declaration.accept(new MethodVisitor(className), null);
    }
  }

  private class MethodVisitor extends VoidVisitorAdapter<Void> {
    private final String className;

    public MethodVisitor(String className) {
      this.className = className;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
      super.visit(methodDeclaration, arg);

      String methodName = className + "." + methodDeclaration.getNameAsString();
      CognitiveMetrics metrics = new CognitiveMetrics(
          className,
          methodDeclaration.getNameAsString()
      );

      methodMetrics.put(methodName, metrics);

      metrics.setArgumentCount(methodDeclaration.getParameters().size());
      metrics.setLineCount(methodDeclaration.getEnd().map(end ->
              end.line - methodDeclaration.getBegin().get().line + 1
          )
          .orElse(1));

      methodDeclaration.accept(new StatementVisitor(metrics, 0), null);
    }
  }
}
