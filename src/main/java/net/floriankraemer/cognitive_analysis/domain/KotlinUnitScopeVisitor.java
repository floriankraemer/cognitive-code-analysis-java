package net.floriankraemer.cognitive_analysis.domain;

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtInstanceExpressionWithLabel;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.psi.KtValueArgumentName;

/**
 * Walks one Kotlin function body including its lambdas and local functions but not the bodies of
 * anonymous objects or local classes, which are skipped like their Java counterparts. Use one
 * instance per body.
 *
 * <p>ponytail: like for Java there is no symbol resolution. A name that starts with an upper-case
 * letter is taken for a type, an object or a constant, and a chain of at least two lower-case names
 * before such a name ({@code java.util} in {@code java.util.List}) for a package.
 *
 * <p>Only code without parse errors is visited, so a qualified expression always has a selector.
 */
abstract class KotlinUnitScopeVisitor extends KtTreeVisitorVoid {

  private static final int MIN_PACKAGE_SEGMENTS = 2;

  @Override
  public void visitClassOrObject(final KtClassOrObject declaration) {
    // skipped
  }

  /**
   * True for a name that stands for a value: a variable, a parameter, a property or a type used as
   * a value. False for the name of a called function, a type, a named argument, a callable reference
   * and the {@code this} or {@code super} keyword.
   */
  static boolean isValueName(final KtNameReferenceExpression name) {
    final PsiElement parent = name.getParent();
    return !(parent instanceof KtUserType
        || parent instanceof KtValueArgumentName
        || parent instanceof KtInstanceExpressionWithLabel
        || parent instanceof KtCallableReferenceExpression reference && reference.getCallableReference() == name
        || parent instanceof KtCallExpression call && call.getCalleeExpression() == name);
  }

  static boolean isTypeName(final String name) {
    return Character.isUpperCase(name.charAt(0));
  }

  /** True for {@code Type} and {@code a.Type}, also when the type is called as a constructor. */
  static boolean isTypeExpression(final KtExpression expression) {
    return switch (expression) {
      case KtNameReferenceExpression name -> isTypeName(name.getReferencedName());
      case KtCallExpression call -> call.getCalleeExpression() instanceof KtNameReferenceExpression callee
          && isTypeName(callee.getReferencedName());
      case KtDotQualifiedExpression qualified -> isTypeExpression(qualified.getSelectorExpression());
      default -> false;
    };
  }

  /**
   * True for {@code java}, {@code java.util} and {@code util} in {@code java.util.List}: part of a
   * chain of at least two lower-case names that is qualified by an upper-case name. A single name, as
   * in {@code other.MAX}, is a variable or property, not a package.
   */
  static boolean isPackageQualifier(final KtExpression expression) {
    KtExpression chain = expression.getParent() instanceof KtDotQualifiedExpression qualified
        && qualified.getSelectorExpression() == expression ? qualified : expression;
    while (chain.getParent() instanceof KtDotQualifiedExpression qualified
        && qualified.getReceiverExpression() == chain
        && qualified.getSelectorExpression() instanceof KtNameReferenceExpression selector
        && !isTypeName(selector.getReferencedName())) {
      chain = qualified;
    }
    return chain.getParent() instanceof KtDotQualifiedExpression qualified
        && qualified.getReceiverExpression() == chain
        && isTypeExpression(qualified.getSelectorExpression())
        && lowerCaseNameSegments(chain) >= MIN_PACKAGE_SEGMENTS;
  }

  /** The number of names in a chain like {@code a.b.c}, or 0 when it holds anything else. */
  private static int lowerCaseNameSegments(final KtExpression expression) {
    if (expression instanceof KtNameReferenceExpression name) {
      return isTypeName(name.getReferencedName()) ? 0 : 1;
    }
    if (expression instanceof KtDotQualifiedExpression qualified
        && qualified.getSelectorExpression() instanceof KtNameReferenceExpression selector
        && !isTypeName(selector.getReferencedName())) {
      final int receiverSegments = lowerCaseNameSegments(qualified.getReceiverExpression());
      return receiverSegments == 0 ? 0 : receiverSegments + 1;
    }
    return 0;
  }
}
