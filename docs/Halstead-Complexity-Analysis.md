# Halstead, Cyclomatic and Understandability

This page explains the three complexity measures that the tool reports next to the cognitive metrics.
The cognitive metrics and their score are described in [Cognitive Complexity Analysis](./Cognitive-Complexity-Analysis.md).
None of these measures enters the score, so they only help to interpret a method.

## Halstead complexity

Halstead complexity counts the operators and operands of a method body.
Maurice Halstead defined the measures in 1977, and they describe the size of the code rather than how hard it is to follow.

### Counts

The tool counts the following tokens in the body of each method or constructor.

* Operators are binary operators (`+`, `&&`, `==`), assignment operators (`=`, `+=`), unary operators, method calls, object creations, the conditional expression `?:` and `instanceof`.
  Each operator is counted by its symbol, a call as `call:name` and an object creation as `new:Type`.
  A prefix unary operator is kept apart from the binary one with the same symbol, so `-x` and `a - b` are different operators.
* Operands are names, accessed field names and literals.
  Literals are counted by their source text, so `"1"` and `1` are different operands.

There is no symbol resolution.
A type name that is used as the scope of a call, such as `Math` in `Math.max(a, b)`, is an operand like a variable.

### Formulas

| Symbol | Meaning |
| --- | --- |
| `n1` | number of distinct operators |
| `n2` | number of distinct operands |
| `N1` | total number of operators |
| `N2` | total number of operands |

The derived values are:

```text
length     N = N1 + N2
vocabulary n = n1 + n2
volume     V = N × log2(n)
difficulty D = (n1 / 2) × (N2 / n2)
effort     E = D × V
```

A method without operands has a difficulty of 0.
A method with an empty vocabulary has a volume of 0.

The tool reports volume, difficulty and effort.
It does not report the estimated number of bugs or the estimated time, which are also defined by Halstead.

### Output

The console shows the columns when `showHalsteadComplexity` is set in the configuration, and the values have two decimals.

* Volume is yellow from 100 and red from 1000.
* Difficulty is yellow from 10 and red from 50.
* Effort is yellow from 500 and red from 5000.

The JSON report has the fields `halsteadVolume`, `halsteadDifficulty` and `halsteadEffort`, rounded to three decimals.
The CSV report has the columns `Halstead Volume`, `Halstead Difficulty` and `Halstead Effort`.
The sort field `halstead` sorts by volume, see [the sort fields](../README.md#analyse).

### Kotlin

Kotlin code gets the same counts.
Kotlin-specific constructs are mapped as described in the [Kotlin mapping](./Cognitive-Complexity-Analysis.md#java-and-kotlin-metric-mapping).
For example, a call of an upper-case name such as `Order(id)` is the operator `new:Order`, and the safe call `?.` is not an operator.

## Cyclomatic complexity

Cyclomatic complexity, introduced by Thomas McCabe in 1976, is the number of linearly independent paths through a method.
The tool starts at 1 and adds 1 for each decision point.

| Decision point | Counted as |
| --- | --- |
| `if`, including `else if` | `if` |
| `case` label of a `switch` | `case`, once per case entry with labels |
| `default` | nothing |
| `while` | `while` |
| `do` … `while` | `doWhile` |
| `for` | `for` |
| enhanced `for` | `forEach` |
| `catch` | `catch` |
| `?:` conditional expression | `ternary` |
| `&&` | `logicalAnd` |
| `\|\|` | `logicalOr` |

Each case entry with labels counts once, so `case A, B ->` counts as one decision point.
The `else` branch adds nothing.
Lambdas belong to the method that contains them, so their decisions count there.

For Kotlin the kinds are `if`, `case`, `while`, `doWhile`, `forEach`, `catch`, `logicalAnd`, `logicalOr` and `elvis`.
Kotlin `when` branches with a condition count as `case`, and the elvis operator `?:` counts as `elvis`.

The JSON report has the fields `cyclomaticComplexity` and `cyclomaticRiskLevel`.
The console shows the value as `n (risk)` when `showCyclomaticComplexity` is set.

### Risk levels

| Complexity | Risk level | Console colour |
| --- | --- | --- |
| 1 to 5 | `low` | none |
| 6 to 10 | `medium` | yellow |
| 11 to 15 | `high` | red |
| above 15 | `very_high` | red |

## Understandability

The understandability column is the cognitive complexity as defined by SonarSource in the white paper "Cognitive Complexity" by G. Ann Campbell.
It differs from the score of this tool, because the score weights the counts of the eight metrics, while the understandability follows the increments of the Sonar specification.
The full rules, including nesting, boolean sequences, labeled jumps and recursion, are in [Understandability](./Cognitive-Complexity-Analysis.md#understandability).

The understandability uses the same risk bands as the cyclomatic complexity: low up to 5, medium up to 10, high up to 15 and very high above.
The console shows the value as `n (risk)` when `showUnderstandability` is set.
The JSON report has the fields `understandability` and `understandabilityRiskLevel`.
