package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.junit.jupiter.api.Test;

/**
 * Asserts every unit of each fixture with all of its counts, so a missing, extra or miscounted
 * method fails. The expected values are worked out by hand from the fixture sources.
 */
class CognitiveMetricsCollectorTest {

  private static final Path FIXTURES = Path.of("src/test/resources/fixtures/cognitive");

  private final CognitiveMetricsCollector collector = new CognitiveMetricsCollector();

  @Test
  void countsIfsElsesAndNestingWithElseIfContinuingItsChain() throws Exception {
    assertThat(collectFixture("IfStatements.java")).isEqualTo("""
        fixtures.cognitive.IfStatements::chain @5 lines=11 args=1 returns=4 vars=0 props=0 ifs=1 nesting=1 elses=3
        fixtures.cognitive.IfStatements::nestedInThenAndElse @17 lines=15 args=3 returns=0 vars=0 props=0 ifs=4 nesting=3 elses=2
        fixtures.cognitive.IfStatements::elseIfBodyNests @33 lines=9 args=1 returns=0 vars=0 props=0 ifs=2 nesting=2 elses=1
        fixtures.cognitive.IfStatements::braceElseIf @43 lines=9 args=1 returns=0 vars=0 props=0 ifs=2 nesting=2 elses=1
        fixtures.cognitive.IfStatements::ifsInLoopsTrySwitchAndLambda @53 lines=39 args=1 returns=0 vars=3 props=0 ifs=7 nesting=1 elses=0
        fixtures.cognitive.IfStatements::ternariesDoNotCount @93 lines=4 args=1 returns=1 vars=1 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.IfStatements::ifInsideLambdaInsideIf @98 lines=9 args=1 returns=0 vars=1 props=0 ifs=2 nesting=2 elses=0
        fixtures.cognitive.IfStatements::log @108 lines=3 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        """);
  }

  @Test
  void countsReturnsOfLambdasButNothingOfAnonymousAndLocalClasses() throws Exception {
    assertThat(collectFixture("Returns.java")).isEqualTo("""
        fixtures.cognitive.Returns::returnsInLambdas @8 lines=10 args=0 returns=3 vars=3 props=0 ifs=1 nesting=1 elses=0
        fixtures.cognitive.Returns::anonymousClassIsSkipped @19 lines=15 args=1 returns=1 vars=2 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Returns::localClassIsSkipped @35 lines=18 args=1 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0
        """);
  }

  @Test
  void findsTheUnitsOfNestedClassesEnumsRecordsAndInterfacesAndKeysOverloadsByTheirTypes() throws Exception {
    assertThat(collectFixture("Units.java")).isEqualTo("""
        fixtures.cognitive.Units::Units() @8 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::Units(int) @11 lines=3 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::abstractMethod @15 lines=1 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::overloaded() @17 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::overloaded(List,Map) @20 lines=2 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::overloaded(String...) @23 lines=2 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::overloaded(int[],java.util.List) @26 lines=2 args=2 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units::generic @29 lines=3 args=1 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.StaticNested::inStatic @34 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.StaticNested.Deeper::inDeeper @38 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Inner::Inner @44 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Color::code @60 lines=3 args=0 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Point::Point(int,int) @66 lines=5 args=0 returns=0 vars=0 props=0 ifs=1 nesting=1 elses=0
        fixtures.cognitive.Units.Point::Point(int) @72 lines=3 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Point::sum @76 lines=3 args=0 returns=1 vars=1 props=2 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Shape::describe @84 lines=3 args=0 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Shape::unit @88 lines=3 args=0 returns=1 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Units.Shape::helper @92 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        """);
  }

  @Test
  void countsNoArgumentsForACompactRecordConstructorButAllForAnExplicitCanonicalOne() throws Exception {
    String code = """
        record Compact(int a, int b, int c) {
          Compact {
            a = Math.max(a, 0);
          }
        }

        record Canonical(int a, int b) {
          Canonical(int a, int b) {
            this.a = Math.max(a, 0);
            this.b = b;
          }
        }
        """;

    assertThat(render(collector.collect(code))).isEqualTo("""
        Compact::Compact @2 lines=3 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        Canonical::Canonical @8 lines=4 args=2 returns=0 vars=1 props=2 ifs=0 nesting=0 elses=0
        """);
  }

  @Test
  void handlesSwitchExpressionsPatternsTextBlocksAndUnnamedVariables() throws Exception {
    assertThat(collectFixture("ModernJava.java")).isEqualTo("""
        fixtures.cognitive.ModernJava::switchExpression @11 lines=13 args=1 returns=1 vars=1 props=0 ifs=1 nesting=1 elses=0
        fixtures.cognitive.ModernJava::patternInstanceof @25 lines=8 args=1 returns=3 vars=2 props=0 ifs=1 nesting=1 elses=1
        fixtures.cognitive.ModernJava::patternSwitch @34 lines=7 args=1 returns=1 vars=2 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.ModernJava::textBlock @42 lines=7 args=0 returns=1 vars=1 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.ModernJava::unnamedVariables @50 lines=9 args=1 returns=0 vars=0 props=0 ifs=1 nesting=1 elses=0
        """);
  }

  @Test
  void skipsTypesAndMethodsWithTheIgnoreTagInAPrecedingComment() throws Exception {
    assertThat(collectFixture("Ignored.java")).isEqualTo("""
        fixtures.cognitive.PartiallyIgnored::kept @46 lines=3 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.PartiallyIgnored.KeptNested::kept @62 lines=2 args=0 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        """);
  }

  @Test
  void countsDistinctVariablesWithoutParametersAndDistinctNonStaticFieldAccesses() throws Exception {
    assertThat(collectFixture("Variables.java")).isEqualTo("""
        fixtures.cognitive.Variables::distinctNames @12 lines=12 args=1 returns=1 vars=3 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Variables::lambdaCatchAndForEachVariables @25 lines=17 args=1 returns=0 vars=4 props=0 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Variables::fieldAccess @43 lines=6 args=1 returns=1 vars=2 props=4 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Variables::thisCountsOnce @50 lines=6 args=0 returns=0 vars=1 props=1 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Variables::shadowsField @57 lines=4 args=0 returns=1 vars=2 props=1 ifs=0 nesting=0 elses=0
        fixtures.cognitive.Variables::helper @62 lines=2 args=1 returns=0 vars=0 props=0 ifs=0 nesting=0 elses=0
        """);
  }

  @Test
  void countsTheScopeAndArgumentsOfAnObjectCreationButNotItsAnonymousBody() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          A holder;
          class B { B(int value) {} }
          Object a() {
            int size = 1;
            return holder.new B(size) {
              int hidden() { return this.secret + other; }
            };
          }
        }
        """).get("A::a");

    // size and this (for the implicit this.holder); the field holder
    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isEqualTo(2);
    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isEqualTo(1);
    assertThat(metrics.value(MetricName.RETURN_COUNT)).isEqualTo(1);
  }

  @Test
  void readsABareNameThatIsNeitherParameterNorLocalAsAnImplicitThisField() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          int count;
          int total;
          int a(int factor) {
            int local = count * factor;
            return local + this.total + total + count;
          }
        }
        """).get("A::a");

    // local and this; count and total, the latter both explicit and implicit
    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isEqualTo(2);
    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isEqualTo(2);
  }

  @Test
  void countsNoThisForAMethodThatReadsOnlyParametersLocalsAndStaticMembers() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          static final int LIMIT = 3;
          int a(int value) {
            int doubled = value * 2;
            helper();
            return Math.min(doubled, LIMIT);
          }
          void helper() {}
        }
        """).get("A::a");

    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isEqualTo(1);
    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isZero();
  }

  @Test
  void treatsANameDeclaredAnywhereInTheMethodAsALocalEverywhereInIt() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          int count;
          int a() {
            int before = count;
            int count = 2;
            return before + count;
          }
        }
        """).get("A::a");

    // the field read in the first line is taken for the later local: no this, no property
    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isEqualTo(2);
    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isZero();
  }

  @Test
  void treatsFullyQualifiedTypesAsNeitherVariablesNorProperties() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          void a() {
            java.lang.System.out.flush();
            java.util.Map.Entry.class.getName();
            Object max = java.lang.Integer.MAX_VALUE;
          }
        }
        """).get("A::a");

    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isEqualTo(1);
    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isZero();
  }

  @Test
  void readsASingleLowerCaseNameBeforeAConstantAsAnImplicitThisField() throws Exception {
    Map<String, CognitiveMetrics> metrics = collector.collect("""
        class A {
          A other;
          A holder;
          static final int MAX = 1;
          static final A INSTANCE = null;
          int fields() {
            return other.MAX + holder.INSTANCE.hashCode();
          }
          int parameterAndLocal(A other) {
            A holder = other;
            return other.MAX + holder.INSTANCE.hashCode();
          }
        }
        """);

    // this; the fields other and holder, but not the constants MAX and INSTANCE
    assertThat(metrics.get("A::fields").value(MetricName.VARIABLE_COUNT)).isEqualTo(1);
    assertThat(metrics.get("A::fields").value(MetricName.PROPERTY_CALL_COUNT)).isEqualTo(2);
    // only the local holder
    assertThat(metrics.get("A::parameterAndLocal").value(MetricName.VARIABLE_COUNT)).isEqualTo(1);
    assertThat(metrics.get("A::parameterAndLocal").value(MetricName.PROPERTY_CALL_COUNT)).isZero();
  }

  @Test
  void readsALowerCaseChainBeforeATypeAsAPackageOnlyWithAtLeastTwoNames() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          Object a(Object value) {
            Object list = java.util.List.of(value);
            return com.acme.Outer.Inner.CONSTANT + list;
          }
        }
        """).get("A::a");

    assertThat(metrics.value(MetricName.VARIABLE_COUNT)).isEqualTo(1);
    assertThat(metrics.value(MetricName.PROPERTY_CALL_COUNT)).isZero();
  }

  @Test
  void skipsLocalInterfacesLikeLocalClasses() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          int a() {
            interface Local {
              default int value() { if (true) { return 1; } return 2; }
            }
            return 0;
          }
        }
        """).get("A::a");

    assertThat(metrics.value(MetricName.RETURN_COUNT)).isEqualTo(1);
    assertThat(metrics.value(MetricName.IF_COUNT)).isZero();
  }

  @Test
  void rejectsALocalEnumAsJavaParserCannotParseIt() {
    assertThatThrownBy(() -> collector.collect("class A {\n  void a() {\n    enum Kind { ONE, TWO }\n  }\n}\n"))
        .isInstanceOf(SourceParseException.class)
        .hasMessageStartingWith("(line 3,col ");
  }

  @Test
  void keysClassesWithoutAPackageByTheirSimpleName() throws Exception {
    assertThat(collector.collect("class A { void a() {} }")).containsOnlyKeys("A::a");
  }

  @Test
  void startsTheLineCountAtTheAnnotationsButNotAtTheJavadoc() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          /**
           * Javadoc.
           */
          @Deprecated
          @SuppressWarnings("unused")
          void a() {
          }
        }
        """).get("A::a");

    assertThat(metrics.getLine()).isEqualTo(5);
    assertThat(metrics.value(MetricName.LINE_COUNT)).isEqualTo(4);
  }

  @Test
  void excludesBodylessInterfaceMethodsButKeepsNativeAndAbstractClassMethods() throws Exception {
    assertThat(collector.collect("""
        interface I { void abstractMethod(); }
        abstract class C { native void nativeMethod(); abstract void abstractMethod(); }
        enum E { ONE; abstract void abstractMethod(); }
        """)).containsOnlyKeys("C::nativeMethod", "C::abstractMethod", "E::abstractMethod");
  }

  @Test
  void ignoresTheTagInAnUnrelatedCommentOfTheFile() throws Exception {
    assertThat(collector.collect("""
        // @cca-ignore in a file header
        package p;

        class A {
          void a() {}
        }
        """)).containsOnlyKeys("p.A::a");
  }

  @Test
  void appliesATrailingTagToTheDeclarationItFollowsOnly() throws Exception {
    assertThat(collector.collect("""
        class A {
          void a() {} // @cca-ignore
          void b() {}
        }
        """)).containsOnlyKeys("A::b");
  }

  @Test
  void ignoresATopLevelClassThatFollowsTheImports() throws Exception {
    assertThat(collector.collect("""
        import java.util.List;
        // @cca-ignore
        class A {
          void a() {}
        }
        class B {
          void b() {}
        }
        """)).containsOnlyKeys("B::b");
  }

  @Test
  void keepsTheFirstOfTwoMethodsWithTheSameErasedSignature() throws Exception {
    CognitiveMetrics metrics = collector.collect("""
        class A {
          void a(java.util.List<String> first) {}
          void a(java.util.List<Integer> second) {
            return;
          }
        }
        """).get("A::a(java.util.List)");

    assertThat(metrics.value(MetricName.RETURN_COUNT)).isZero();
  }

  @Test
  void rejectsInvalidCodeWithTheLocationOfTheFirstProblem() {
    assertThatThrownBy(() -> collector.collect("class Broken {\n  void oops( {\n}\n"))
        .isInstanceOf(SourceParseException.class)
        .hasMessageStartingWith("(line 2,col 12) Parse error. Found \"{\"");
  }

  @Test
  void canBeUsedFromSeveralThreadsAtOnce() throws Exception {
    String code = Files.readString(FIXTURES.resolve("IfStatements.java"));
    String expected = render(collector.collect(code));

    var results = java.util.stream.IntStream.range(0, 16).parallel()
        .mapToObj(i -> {
          try {
            return render(collector.collect(code));
          } catch (SourceParseException e) {
            throw new IllegalStateException(e);
          }
        })
        .collect(Collectors.toSet());

    assertThat(results).containsExactly(expected);
  }

  private String collectFixture(String fileName) throws IOException, SourceParseException {
    return render(collector.collect(Files.readString(FIXTURES.resolve(fileName))));
  }

  private static String render(Map<String, CognitiveMetrics> metrics) {
    return metrics.entrySet().stream()
        .map(entry -> {
          CognitiveMetrics m = entry.getValue();
          return String.format(Locale.ROOT,
              "%s @%d lines=%d args=%d returns=%d vars=%d props=%d ifs=%d nesting=%d elses=%d%n",
              entry.getKey(), m.getLine(),
              m.value(MetricName.LINE_COUNT), m.value(MetricName.ARG_COUNT),
              m.value(MetricName.RETURN_COUNT), m.value(MetricName.VARIABLE_COUNT),
              m.value(MetricName.PROPERTY_CALL_COUNT), m.value(MetricName.IF_COUNT),
              m.value(MetricName.IF_NESTING_LEVEL), m.value(MetricName.ELSE_COUNT));
        })
        .collect(Collectors.joining())
        .replace("\r\n", "\n");
  }
}
