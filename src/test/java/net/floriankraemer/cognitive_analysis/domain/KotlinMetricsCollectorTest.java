package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.junit.jupiter.api.Test;

/**
 * Asserts every unit of each Kotlin fixture with all of its metrics, so a missing, extra or miscounted
 * function fails. The expected values are worked out by hand from the fixture sources.
 *
 * <p>A line reads: the key, the first line, the 8 cognitive counts, then {@code cc} with the cyclomatic
 * total, base, if, case, while, doWhile, forEach, catch, logicalAnd, logicalOr and elvis; {@code u} with
 * the understandability total, structural, hybrid, fundamental, nesting and recursion; and {@code h}
 * with the Halstead n1/n2/N1/N2.
 */
class KotlinMetricsCollectorTest {

  private static final Path FIXTURES = Path.of("src/test/resources/fixtures/kotlin/cognitive");

  private final KotlinMetricsCollector collector = new KotlinMetricsCollector();

  @Test
  void countsIfsElsesAndNestingWithElseIfContinuingItsChain() throws Exception {
    assertThat(collectFixture("IfStatements.kt")).isEqualTo("""
        fixtures.kotlin.IfStatements::chain @5 lines=11 args=1 returns=4 vars=0 props=0 ifs=1 nesting=1 elses=3 cc=[4, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0] u=[4, 1, 3, 0, 0, 0] h=1/5/3/10
        fixtures.kotlin.IfStatements::ifExpression @17 lines=4 args=1 returns=1 vars=1 props=0 ifs=1 nesting=1 elses=2 cc=[3, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0] u=[3, 1, 2, 0, 0, 0] h=2/6/2/8
        fixtures.kotlin.IfStatements::nestedInThenAndElse @22 lines=11 args=3 returns=0 vars=0 props=0 ifs=4 nesting=3 elses=2 cc=[5, 1, 4, 0, 0, 0, 0, 0, 0, 0, 0] u=[10, 4, 2, 0, 4, 0] h=1/6/3/7
        fixtures.kotlin.IfStatements::braceElseIf @34 lines=9 args=1 returns=0 vars=0 props=0 ifs=2 nesting=2 elses=1 cc=[3, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0] u=[4, 2, 1, 0, 1, 0] h=2/5/4/6
        fixtures.kotlin.IfStatements::ifInsideLambdaInsideIf @44 lines=7 args=1 returns=0 vars=1 props=0 ifs=2 nesting=2 elses=0 cc=[3, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0] u=[4, 2, 0, 0, 2, 0] h=4/4/4/5
        fixtures.kotlin.IfStatements::ifsInLoopsWhenAndTry @52 lines=14 args=1 returns=0 vars=2 props=2 ifs=4 nesting=1 elses=0 cc=[8, 1, 4, 1, 0, 0, 1, 1, 0, 0, 0] u=[10, 7, 0, 0, 3, 0] h=5/13/9/17
        fixtures.kotlin.IfStatements::log @67 lines=1 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/1/1/1
        """);
    // ifExpression: the if expression and its else-if chain count like statements; sign is the variable.
    // ifInsideLambdaInsideIf: the implicit lambda parameter it is a variable.
    // ifsInLoopsWhenAndTry: value and e; the properties size and message. The when branch 0 is one case.
  }

  @Test
  void countsLabeledReturnsAndLocalFunctionsButNothingOfAnonymousObjectsAndLocalClasses() throws Exception {
    assertThat(collectFixture("Returns.kt")).isEqualTo("""
        fixtures.kotlin.Returns::returnsInLambdas @5 lines=8 args=1 returns=3 vars=3 props=0 ifs=1 nesting=1 elses=0 cc=[3, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1] u=[2, 1, 0, 0, 1, 0] h=6/5/6/9
        fixtures.kotlin.Returns::expressionBodyIsNoReturn @14 lines=1 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/2/1/2
        fixtures.kotlin.Returns::anonymousObjectIsSkipped @16 lines=11 args=1 returns=1 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.Returns::localClassIsSkipped @28 lines=9 args=1 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=2/0/2/0
        fixtures.kotlin.Returns::localFunctionBelongsToTheFunction @38 lines=7 args=1 returns=3 vars=1 props=0 ifs=1 nesting=1 elses=0 cc=[2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0] u=[2, 1, 0, 0, 1, 0] h=2/3/2/5
        """);
    // returnsInLambdas: return@forEach, return@firstOrNull and return; the variables value, first and it.
    // localFunctionBelongsToTheFunction: the three returns and the parameter limit of the local function.
  }

  @Test
  void keysClassesObjectsCompanionsTopLevelAndExtensionFunctionsAndOverloads() throws Exception {
    assertThat(collectFixture("Units.kt")).isEqualTo("""
        fixtures.kotlin.UnitsKt::topLevel @3 lines=3 args=1 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.UnitsKt::shout @7 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/0/1/0
        fixtures.kotlin.UnitsKt::overloadedTopLevel(Int) @9 lines=1 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.UnitsKt::overloadedTopLevel(String,List...) @11 lines=1 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.UnitsKt::overloadedTopLevel(Int,Function1) @13 lines=1 args=1 returns=0 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/0/1/0
        fixtures.kotlin.Units::Units() @17 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units::Units(String) @19 lines=5 args=1 returns=0 vars=0 props=0 ifs=1 nesting=1 elses=0 cc=[2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0] u=[1, 1, 0, 0, 0, 0] h=2/2/2/2
        fixtures.kotlin.Units::abstractFunction @29 lines=1 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units::overloaded() @31 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units::overloaded(List,Map) @34 lines=2 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units::overloaded(IntArray,kotlin.collections.List) @37 lines=2 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units::extensionMember @40 lines=1 args=0 returns=0 vars=1 props=1 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/1/1/1
        fixtures.kotlin.Units.Nested::inNested @49 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units.Nested.Deeper::inDeeper @53 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Units.Companion::create @59 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.Registry::register @64 lines=3 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/1/1/1
        fixtures.kotlin.NamedCompanion.Factory::build @71 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/0/1/0
        fixtures.kotlin.Shape::describe @78 lines=3 args=0 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=2/1/2/1
        fixtures.kotlin.Color::code @89 lines=3 args=0 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.Point::sum @95 lines=1 args=0 returns=0 vars=1 props=2 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/2/1/2
        """);
    // Left out: the primary constructor, the init block, the property getter, the bodyless interface
    // function area and the body of the enum entry RED.
    // An extension receiver is the first parameter type of an overload; this in an extension is the receiver.
  }

  @Test
  void namesTheFacadeClassOfTopLevelFunctionsAfterJvmName() throws Exception {
    assertThat(collectFixture("JvmName.kt")).isEqualTo("""
        fixtures.kotlin.Tools::helper @5 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        fixtures.kotlin.InJvmNameFile::member @8 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        """);
  }

  @Test
  void countsDistinctVariablesWithoutParametersAndDistinctPropertyAccesses() throws Exception {
    assertThat(collectFixture("Variables.kt")).isEqualTo("""
        fixtures.kotlin.Variables::distinctNames @7 lines=13 args=1 returns=1 vars=4 props=0 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0] u=[2, 2, 0, 0, 0, 0] h=5/9/7/14
        fixtures.kotlin.Variables::destructuringLambdaCatchAndForVariables @21 lines=13 args=1 returns=0 vars=7 props=0 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0] u=[2, 2, 0, 0, 0, 0] h=7/10/11/13
        fixtures.kotlin.Variables::propertyAccess @35 lines=4 args=1 returns=1 vars=2 props=3 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=3/7/7/11
        fixtures.kotlin.Variables::implicitThis @40 lines=4 args=1 returns=1 vars=1 props=2 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=2/3/2/4
        fixtures.kotlin.Variables::shadowsProperty @45 lines=4 args=0 returns=1 vars=2 props=1 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/3/1/3
        fixtures.kotlin.Variables::onlyParametersAndConstants @50 lines=4 args=1 returns=1 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=2/8/2/8
        fixtures.kotlin.Variables::itWithoutLambdaIsAProperty @55 lines=1 args=0 returns=0 vars=1 props=1 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/1/0/1
        """);
    // distinctNames: x, y, x2 and i, which two loops declare.
    // destructuring...: first, second, key, value, k (not _), it and e.
    // propertyAccess: local and this; total (holder.total, this.total and other!!.total), the implicit
    // this.other and count (other?.count), but not Companion.LIMIT.
    // shadowsProperty: the local count and this for the implicit this.total.
    // onlyParametersAndConstants: doubled; java.lang is a package, LIMIT, Math and Int are no properties.
    // itWithoutLambdaIsAProperty: it outside a lambda is the property it, read as this.it.
  }

  @Test
  void skipsDeclarationsWithTheIgnoreTagInABoundOrPrecedingComment() throws Exception {
    assertThat(collectFixture("Ignored.kt")).isEqualTo("""
        fixtures.kotlin.IgnoredKt::keptAfterATrailingComment @18 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.PartiallyIgnored::kept @42 lines=3 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.PartiallyIgnored.KeptNested::kept @59 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        """);
  }

  @Test
  void countsDecisionPointsWithWhenBranchesAndElvis() throws Exception {
    assertThat(collectFixture("Decisions.kt")).isEqualTo("""
        fixtures.kotlin.Decisions::empty @5 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Decisions::elseIfChain @7 lines=3 args=1 returns=0 vars=0 props=0 ifs=1 nesting=1 elses=2 cc=[3, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0] u=[3, 1, 2, 0, 0, 0] h=2/2/2/4
        fixtures.kotlin.Decisions::whenWithSubject @11 lines=6 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[4, 1, 0, 3, 0, 0, 0, 0, 0, 0, 0] u=[1, 1, 0, 0, 0, 0] h=3/10/3/10
        fixtures.kotlin.Decisions::whenWithoutSubject @18 lines=7 args=1 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0] u=[1, 1, 0, 0, 0, 0] h=2/5/2/7
        fixtures.kotlin.Decisions::loops @26 lines=5 args=1 returns=0 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[4, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0] u=[3, 3, 0, 0, 0, 0] h=1/1/2/3
        fixtures.kotlin.Decisions::tryCatches @32 lines=8 args=0 returns=0 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0] u=[2, 2, 0, 0, 0, 0] h=1/0/1/0
        fixtures.kotlin.Decisions::logical @41 lines=1 args=3 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[4, 1, 0, 0, 0, 0, 0, 0, 2, 1, 0] u=[3, 0, 0, 3, 0, 0] h=3/3/4/4
        fixtures.kotlin.Decisions::nullHandling @43 lines=4 args=1 returns=1 vars=1 props=1 ifs=0 nesting=0 elses=0 cc=[2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1] u=[0, 0, 0, 0, 0, 0] h=4/3/4/6
        fixtures.kotlin.Decisions::lambdasAddNothing @48 lines=1 args=1 returns=0 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=4/4/4/5
        """);
    // whenWithSubject: the branches "1, 2, 3", "in 4..9" and "is Int" count, else does not.
    // nullHandling: only the elvis counts; ?. and !! add nothing.
  }

  @Test
  void measuresUnderstandabilityWithWhenOnceLambdasNestingAndRecursion() throws Exception {
    assertThat(collectFixture("Understandability.kt")).isEqualTo("""
        fixtures.kotlin.Understandability::nesting @5 lines=9 args=1 returns=0 vars=1 props=0 ifs=1 nesting=1 elses=0 cc=[4, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0] u=[6, 3, 0, 0, 3, 0] h=2/4/3/6
        fixtures.kotlin.Understandability::whenCountsOnce @15 lines=7 args=2 returns=0 vars=0 props=0 ifs=1 nesting=1 elses=0 cc=[4, 1, 1, 2, 0, 0, 0, 0, 0, 0, 0] u=[3, 2, 0, 0, 1, 0] h=1/7/3/7
        fixtures.kotlin.Understandability::labeledJumps @23 lines=9 args=1 returns=0 vars=2 props=0 ifs=3 nesting=1 elses=0 cc=[6, 1, 3, 0, 0, 0, 2, 0, 0, 0, 0] u=[14, 5, 0, 2, 7, 0] h=3/6/3/8
        fixtures.kotlin.Understandability::lambdaNests @33 lines=5 args=1 returns=0 vars=1 props=0 ifs=1 nesting=1 elses=0 cc=[2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0] u=[2, 1, 0, 0, 1, 0] h=3/3/3/4
        fixtures.kotlin.Understandability::localFunctionNests @39 lines=7 args=1 returns=3 vars=0 props=0 ifs=1 nesting=1 elses=0 cc=[2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0] u=[2, 1, 0, 0, 1, 0] h=3/3/3/5
        fixtures.kotlin.Understandability::sequences @47 lines=3 args=4 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[6, 1, 0, 0, 0, 0, 0, 0, 3, 2, 0] u=[3, 0, 0, 3, 0, 0] h=3/4/6/6
        fixtures.kotlin.Understandability::elvisAddsNothing @51 lines=1 args=1 returns=0 vars=0 props=1 ifs=0 nesting=0 elses=0 cc=[2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1] u=[0, 0, 0, 0, 0, 0] h=2/3/2/3
        fixtures.kotlin.Understandability::factorial @53 lines=1 args=1 returns=0 vars=0 props=0 ifs=1 nesting=1 elses=1 cc=[2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0] u=[3, 1, 1, 1, 0, 1] h=4/2/4/6
        fixtures.kotlin.Understandability::recursionOnThis @55 lines=3 args=1 returns=1 vars=1 props=0 ifs=1 nesting=1 elses=1 cc=[2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0] u=[3, 1, 1, 1, 0, 1] h=4/3/5/6
        fixtures.kotlin.Understandability::noRecursionOnOtherReceiver @59 lines=3 args=1 returns=0 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/1/1/1
        fixtures.kotlin.Understandability::tryCatch @63 lines=7 args=0 returns=0 vars=1 props=1 ifs=1 nesting=1 elses=0 cc=[3, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0] u=[3, 2, 0, 0, 1, 0] h=2/3/3/4
        fixtures.kotlin.Understandability::doWhileLoop @71 lines=5 args=1 returns=0 vars=0 props=1 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0] u=[2, 1, 0, 1, 0, 0] h=4/3/4/5
        """);
    // labeledJumps: two loops and three ifs at nesting 0, 1, 2, 2, 2; continue@outer and break@outer.
    // sequences: a && b && (c || d) || !(a && d) has the runs && and || and the negated run &&.
  }

  @Test
  void collectsTheOperatorsAndOperandsOfKotlinExpressions() throws Exception {
    assertThat(collectFixture("Operations.kt")).isEqualTo("""
        fixtures.kotlin.Operations::empty @5 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/0/0/0
        fixtures.kotlin.Operations::add @7 lines=1 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/2/1/2
        fixtures.kotlin.Operations::assignmentsAndUnary @9 lines=7 args=1 returns=1 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=4/3/4/7
        fixtures.kotlin.Operations::chainedCalls @17 lines=1 args=1 returns=0 vars=0 props=1 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/2/1/2
        fixtures.kotlin.Operations::creation @19 lines=1 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/1/1/1
        fixtures.kotlin.Operations::nullSafety @21 lines=1 args=1 returns=0 vars=0 props=1 ifs=0 nesting=0 elses=0 cc=[2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1] u=[0, 0, 0, 0, 0, 0] h=4/2/4/3
        fixtures.kotlin.Operations::typeChecksAndRanges @23 lines=1 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 0, 0, 0, 0, 0, 2, 0, 0] u=[1, 0, 0, 1, 0, 0] h=5/4/6/5
        fixtures.kotlin.Operations::literals @25 lines=1 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/7/1/7
        fixtures.kotlin.Operations::templates @27 lines=1 args=1 returns=0 vars=0 props=1 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=0/5/0/6
        fixtures.kotlin.Operations::lambdasAndReferences @29 lines=1 args=1 returns=0 vars=1 props=0 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=4/3/4/3
        fixtures.kotlin.Operations::infixCall @31 lines=1 args=1 returns=0 vars=1 props=1 ifs=0 nesting=0 elses=0 cc=[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0] u=[0, 0, 0, 0, 0, 0] h=1/2/1/2
        fixtures.kotlin.Operations::whenConditions @33 lines=5 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0 cc=[3, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0] u=[1, 1, 0, 0, 0, 0] h=3/4/3/6
        """);
    // assignmentsAndUnary: +=, =, u- and u++; operands a, x, 1, x, x, x, x (val x = a has no operator).
    // chainedCalls: call:trim; operands s and length.
    // creation: StringBuilder(n) is new:StringBuilder; operand n.
    // nullSafety: ?., ?:, u!! and call:hashCode; operands text, length, text.
    // typeChecksAndRanges: is, &&, in, .., &&, !is; operands o, i, 1, 9, o.
    // literals: call:listOf; operands "s", 'c', 1, 2L, 1.5, true, null.
    // templates: the literal parts "hello ", " and ", "!" and the names name, name, length.
    // lambdasAndReferences: call:map, *, call:forEach and the reference call:println; operands list, x, 2.
    // infixCall: a to count is call:to; operands a and count.
    // whenConditions: is, in, ..; operands o, 1, 2, 3 with 1 and 2 twice.
  }

  @Test
  void measuresEquivalentJavaAndKotlinCodeTheSame() throws Exception {
    final Path fixtures = Path.of("src/test/resources/fixtures/kotlin/crosscheck");
    final Map<String, CognitiveMetrics> java =
        new CognitiveMetricsCollector().collect(Files.readString(fixtures.resolve("Equivalent.java")));
    final Map<String, CognitiveMetrics> kotlin =
        collector.collect("Equivalent.kt", Files.readString(fixtures.resolve("Equivalent.kt")));

    assertThat(kotlin).hasSize(12);
    assertThat(renderComparable(kotlin)).isEqualTo(renderComparable(java));
  }

  @Test
  void keysTopLevelFunctionsWithoutAPackageByTheFacadeClassOfTheFileName() throws Exception {
    assertThat(collector.collect("my-tools.kt", "fun a() {}\nclass A { fun b() {} }\n"))
        .containsOnlyKeys("My_toolsKt::a", "A::b");
  }

  @Test
  void ignoresTheTagInAFileHeader() throws Exception {
    assertThat(collector.collect("A.kt", """
        // @cca-ignore in a file header
        package p

        class A {
            fun a() {}
        }
        """)).containsOnlyKeys("p.A::a");
  }

  @Test
  void startsTheLineCountAtTheAnnotationsButNotAtTheKDoc() throws Exception {
    final CognitiveMetrics metrics = collector.collect("A.kt", """
        class A {
            /**
             * KDoc.
             */
            @Deprecated("old")
            @Suppress("unused")
            fun a() {
            }
        }
        """).get("A::a");

    assertThat(metrics.getLine()).isEqualTo(5);
    assertThat(metrics.value(MetricName.LINE_COUNT)).isEqualTo(4);
  }

  @Test
  void readsWindowsLineEndingsLikeUnixOnes() throws Exception {
    final String unix = Files.readString(FIXTURES.resolve("IfStatements.kt"));

    assertThat(render(collector.collect("IfStatements.kt", unix.replace("\n", "\r\n"))))
        .isEqualTo(render(collector.collect("IfStatements.kt", unix)));
  }

  @Test
  void excludesBodylessInterfaceFunctionsButKeepsAbstractAndExternalClassFunctions() throws Exception {
    assertThat(collector.collect("A.kt", """
        interface I { fun abstractFunction() }
        abstract class C { external fun nativeFunction(); abstract fun abstractFunction() }
        """)).containsOnlyKeys("C::nativeFunction", "C::abstractFunction");
  }

  @Test
  void keepsTheFirstOfTwoFunctionsWithTheSameErasedSignature() throws Exception {
    final CognitiveMetrics metrics = collector.collect("A.kt", """
        class A {
            fun a(first: List<String>) {}
            fun a(second: List<Int>?) {
                return
            }
        }
        """).get("A::a(List)");

    assertThat(metrics.value(MetricName.RETURN_COUNT)).isZero();
  }

  @Test
  void treatsAChainOfLowerCaseNamesBeforeACallAsPropertiesAndNotAsAPackage() throws Exception {
    final CognitiveMetrics metrics = collector.collect("A.kt", """
        class A {
            fun a(values: List<Int>) = values.size.toString() + java.util.ArrayList<Int>().size
        }
        """).get("A::a");

    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isEqualTo(1);
    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isZero();
  }

  @Test
  void visitsTheConditionsAndNestsTheBodiesOfWhileAndDoWhileLoops() throws Exception {
    final Map<String, CognitiveMetrics> metrics = collector.collect("A.kt", """
        class A {
            fun loops(a: Boolean, b: Boolean) {
                while (a && b) {
                    if (a) println()
                }
                do {
                    if (b) println()
                } while (a)
            }
        }
        """);

    final CognitiveMetrics loops = metrics.get("A::loops");
    // while (1), && (1), if at nesting 1 (2), do-while (1), if at nesting 1 (2)
    assertThat(loops.getUnderstandability().complexity()).isEqualTo(7);
    // 1 + while, do-while, two ifs and &&
    assertThat(loops.getCyclomatic().complexity()).isEqualTo(6);
  }

  @Test
  void countsALabeledBreakAndNoRecursionOnTheThisOfAnotherClass() throws Exception {
    final Map<String, CognitiveMetrics> metrics = collector.collect("A.kt", """
        class A {
            fun jump(rows: List<Int>) {
                outer@ for (row in rows) {
                    break@outer
                }
            }
            inner class B {
                fun jump(rows: List<Int>) {
                    this@A.jump(rows)
                }
            }
        }
        """);

    assertThat(metrics.get("A::jump").getUnderstandability().breakdown())
        .containsEntry("structural", 1).containsEntry("fundamental", 1);
    assertThat(metrics.get("A.B::jump").getUnderstandability().complexity()).isZero();
  }

  @Test
  void namesNegatedTypeAndRangeChecksAsOperatorsOfTheirOwn() throws Exception {
    final HalsteadMetrics halstead = collector.collect("A.kt", """
        class A {
            fun a(o: Any) = when (o) {
                !is String -> 1
                !in 1..2 -> 2
                else -> 3
            }
        }
        """).get("A::a").getHalstead();

    // !is, !in and ..; then !is, && and is
    assertThat(halstead.distinctOperators()).isEqualTo(3);
    assertThat(collector.collect("A.kt", "class A { fun a(o: Any) = o !is String && o is Int }")
        .get("A::a").getHalstead().distinctOperators()).isEqualTo(3);
  }

  @Test
  void namesTypeAndRangeChecksTheSameInExpressionsAndWhenBranches() throws Exception {
    final Map<String, CognitiveMetrics> metrics = collector.collect("A.kt", """
        class A {
            fun checks(o: Any) = when (o) {
                is String -> o is CharSequence
                in 1..2 -> o in 3..4
                else -> false
            }
            fun negatedChecks(o: Any) = when (o) {
                !is String -> o !is CharSequence
                !in 1..2 -> o !in 3..4
                else -> false
            }
        }
        """);

    // is, in and .. in both places
    assertThat(metrics.get("A::checks").getHalstead().distinctOperators()).isEqualTo(3);
    assertThat(metrics.get("A::negatedChecks").getHalstead().distinctOperators()).isEqualTo(3);
  }

  @Test
  void ignoresByCommentsBetweenAnnotationsBeforeTheNameAndBeforeTheFirstDeclarationOfAFile() throws Exception {
    assertThat(collector.collect("A.kt", """
        // @cca-ignore
        class Hidden {
            fun hidden() {}
        }
        class A {
            @Deprecated("x") /* @cca-ignore */ @Suppress("y") fun betweenAnnotations() {}
            fun /* @cca-ignore */ beforeTheName() {}
            fun kept() {}
        }
        """)).containsOnlyKeys("A::kept");
  }

  @Test
  void appliesAnUnboundCommentOnTheLineOfThePreviousDeclarationToThatDeclarationOnly() throws Exception {
    assertThat(collector.collect("A.kt", """
        class A {
            fun a() {} /* @cca-ignore */

            fun b() {}
        }
        """)).containsOnlyKeys("A::b");
  }

  @Test
  void leavesAClassAloneWhenTheTagTrailsThePackageDirectiveOnItsLine() throws Exception {
    assertThat(collector.collect("A.kt", """
        package p // @cca-ignore trails the package directive
        class A {
            fun a() {}
        }
        """)).containsOnlyKeys("p.A::a");
  }

  @Test
  void countsTheReceiverOfACallableReferenceAsAnOperand() throws Exception {
    final HalsteadMetrics halstead = collector.collect("A.kt", """
        class A {
            fun a(list: MutableList<Int>, values: List<Int>) = values.forEach(list::add)
        }
        """).get("A::a").getHalstead();

    // call:forEach and call:add; values and list
    assertThat(halstead.totalOperators()).isEqualTo(2);
    assertThat(halstead.totalOperands()).isEqualTo(2);
  }

  @Test
  void countsTheReceiverOfAFunctionTypeAsAParameterOfItsErasure() throws Exception {
    assertThat(collector.collect("A.kt", """
        class A {
            fun a(block: Int.(String) -> Unit) {}
            fun a(block: (String) -> Unit) {}
        }
        """)).containsOnlyKeys("A::a(Function2)", "A::a(Function1)");
  }

  @Test
  void rejectsInvalidCodeWithTheLocationOfTheFirstProblem() {
    assertThatThrownBy(() -> collector.collect("Broken.kt", "class Broken {\n    fun oops( {\n    }\n}\n"))
        .isInstanceOf(SourceParseException.class)
        .hasMessage("(line 2,col 14) Expecting ')'");
  }

  @Test
  void canBeUsedFromSeveralThreadsAtOnce() throws Exception {
    final String code = Files.readString(FIXTURES.resolve("Variables.kt"));
    final String expected = render(collector.collect("Variables.kt", code));

    final var results = IntStream.range(0, 16).parallel()
        .mapToObj(i -> {
          try {
            return render(collector.collect("Variables.kt", code));
          } catch (SourceParseException e) {
            throw new IllegalStateException(e);
          }
        })
        .collect(Collectors.toSet());

    assertThat(results).containsExactly(expected);
  }

  private String collectFixture(final String fileName) throws IOException, SourceParseException {
    return render(collector.collect(fileName, Files.readString(FIXTURES.resolve(fileName))));
  }

  static String render(final Map<String, CognitiveMetrics> metrics) {
    return metrics.entrySet().stream()
        .map(entry -> {
          final CognitiveMetrics m = entry.getValue();
          return String.format(Locale.ROOT, "%s cc=%s u=%s h=%s%n",
              cognitive(entry.getKey(), m), m.getCyclomatic().breakdown().values(),
              m.getUnderstandability().breakdown().values(), halstead(m.getHalstead()));
        })
        .collect(Collectors.joining());
  }

  /** Leaves out the cyclomatic breakdown, whose kinds differ: Java has ternaries, Kotlin elvis. */
  private static String renderComparable(final Map<String, CognitiveMetrics> metrics) {
    return metrics.entrySet().stream()
        .map(entry -> {
          final CognitiveMetrics m = entry.getValue();
          return String.format(Locale.ROOT, "%s cc=%d u=%s h=%s%n",
              cognitive(entry.getKey(), m), m.getCyclomatic().complexity(),
              m.getUnderstandability().breakdown().values(), halstead(m.getHalstead()));
        })
        .collect(Collectors.joining());
  }

  private static String cognitive(final String key, final CognitiveMetrics m) {
    return String.format(Locale.ROOT,
        "%s @%d lines=%d args=%d returns=%d vars=%d props=%d ifs=%d nesting=%d elses=%d",
        key, m.getLine(),
        m.value(MetricName.LINE_COUNT), m.value(MetricName.ARG_COUNT),
        m.value(MetricName.RETURN_COUNT), m.value(MetricName.VARIABLE_COUNT),
        m.value(MetricName.PROPERTY_CALL_COUNT), m.value(MetricName.IF_COUNT),
        m.value(MetricName.IF_NESTING_LEVEL), m.value(MetricName.ELSE_COUNT));
  }

  private static String halstead(final HalsteadMetrics h) {
    return h.distinctOperators() + "/" + h.distinctOperands() + "/" + h.totalOperators() + "/" + h.totalOperands();
  }
}
