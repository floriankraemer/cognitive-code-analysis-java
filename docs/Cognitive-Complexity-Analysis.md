# Cognitive Complexity Analysis

## Why bother?

Code that is hard to read takes longer to change, and it is more likely to hide bugs.
Cognitive complexity points at the methods where a reader has to hold many things in mind at once.

* **Maintenance**: Most of a developer's time goes into reading and reasoning about existing code, so methods that are easier to follow save time on every change.
* **Correctness**: Deeply nested and branch-heavy code is harder to test and review, which makes defects more likely.
* **Onboarding**: New team members learn a codebase faster when its methods stay small and flat.

## What is the difference to Cyclomatic Complexity?

**Cyclomatic complexity**, introduced by Thomas McCabe in 1976, measures the number of linearly independent paths through a program.
It's based on the control flow graph of the code, where each decision point (like if, while, for, etc.) increases the complexity.

**Cognitive complexity**, is a more recent metric designed to better align with how human brains process code.
It penalizes structures that are difficult for humans to understand, focusing on the mental effort required to read and understand code.

**Cognitive complexity measures how hard it is for a human to understand the code, while cyclomatic complexity measures how hard your code is to test.**

## How is Cognitive Complexity calculated?

Each metric adds a weight to the score of a method.
The weight grows logarithmically once the value of the metric exceeds its threshold, and it is 0 at or below the threshold.

```text
weight = ln(1 + (value - threshold) / scale)     when value > threshold, else 0
```

The weights of the enabled metrics are summed and rounded half-up to three decimals.
A metric whose `enabled` flag is `false` is left out of the score.

For example, `ifNestingLevel` has the threshold 1 and the scale 1.0.
A method with a nesting level of 3 gets `ln(1 + (3 - 1) / 1.0) = ln 3 = 1.099`, which is the value the console shows as `3 (1.099)`.
The defaults and their meaning are described in [Tuning the calculation](./Configuration.md#tuning-the-calculation).

## Metrics Collected

All counts are per method or constructor.
The rules below are written for Java; [Kotlin](#kotlin) maps its constructs onto them.
Lambdas belong to the method that contains them.
Anonymous and local classes are left out, both as units of their own and from the counts of the enclosing method.

- **Line Count**:
  - **Description**: The number of lines from the first annotation or modifier of the declaration to its closing brace.
    Javadoc is not counted.
  - **Purpose**: Measures the size of the method in terms of lines of code, which can give an indication of its complexity and readability.

- **Argument Count**:
  - **Description**: The number of parameters of the method.
    A compact record constructor declares no parameters, so it counts 0; its record components are implicit and are not variables of its body.
  - **Purpose**: High argument counts can indicate that a method is doing too much or is highly dependent on external inputs, which may be a sign that the method needs refactoring.

- **Return Count**:
  - **Description**: The number of `return` statements, including those inside lambdas.
  - **Purpose**: Multiple return points can make a method harder to follow and maintain.
    This metric helps identify methods that may benefit from being refactored for clarity.

- **Variable Count**:
  - **Description**: The number of distinct variable names declared in the method body, without the method parameters, plus `this`.
    Local variables, `for` and for-each variables, lambda and catch parameters, pattern variables and `try` resources count.
    A name that is reused, for example in two loops, counts once; the unnamed variable `_` does not count.
    `this` counts once when the method reads an instance member, either as `this.x` or as a bare field name (see Property Call Count).
  - **Purpose**: A high variable count can indicate a method that is doing too much or is overly complex.
    Monitoring this metric helps to ensure methods remain simple and focused.

- **Property Call Count**:
  - **Description**: The number of distinct field names accessed with a dot, such as `this.x`, `other.x` or `a.b.x`, or read as a bare name.
    A bare name that is neither a parameter nor declared anywhere in the method is read as `this.x`, as PHP writes it with `$this->x`.
    Static accesses on a type, such as `System.out` or `Integer.MAX_VALUE`, do not count.
  - **Limits**: Names are not resolved against the classpath.
    A name starting with an upper-case letter is taken for a type or a constant and counts neither as a variable nor as a field.
    A chain of at least two lower-case names before an upper-case name, as `java.util` in `java.util.List`, is taken for a package.
    A single lower-case name before an upper-case one, as `other` in `other.MAX`, is a parameter, a local or an implicit `this.other`.
    A name declared anywhere in the method is a local everywhere in it, so a field read before a local of the same name is declared counts as that local.
    `array.length` counts as a field access.
  - **Purpose**: Frequent property accesses may indicate a method that is highly coupled to the internal state of the object, which can make the code harder to test and maintain.

- **If Count**:
  - **Description**: The number of `if` statements.
    An `else if` continues its chain and does not count.
    A conditional expression (`a ? b : c`) does not count.
  - **Purpose**: Like else count, this measures the complexity of branching logic.
    A high if count can make code harder to understand and maintain.

- **If Nesting Level**:
  - **Description**: The maximum depth of `if` statements nested in the branches of other `if` statements.
    An `else if` stays on the depth of its chain.
    Loops, `try` and `switch` do not add depth.
  - **Purpose**: Deeply nested `if` statements can be difficult to follow and may indicate a need for refactoring into smaller, more manageable pieces.

- **Else Count**:
  - **Description**: The number of `else` and `else if` branches.
  - **Purpose**: Helps measure the complexity of branching logic.
    A high else count might indicate complex or tightly coupled conditional logic.

## Understandability

Understandability is the cognitive complexity as specified by SonarSource in the white paper "Cognitive Complexity" by G. Ann Campbell.
It is measured per method next to the metrics above and does not enter the score.

- **Structural increments** add 1 plus the current nesting level and nest their body: `if`, the conditional expression `?:`, `switch` (once, not per case), `for`, enhanced `for`, `while`, `do`-`while` and `catch`.
- **Hybrid increments** add 1 without a nesting penalty: `else` and `else if`.
  The body of an `else if` is nested on the level of its `if`, not one level deeper.
- **Lambdas** raise the nesting level of their body without an increment.
- **Fundamental increments** add 1 each:
  - every run of like operators in a sequence of `&&` and `||`, so `a && b && c` is 1 and `a && b || c` is 2.
    Parentheses do not break a sequence, while a negation `!(...)` or a method argument starts a new one;
  - every `break` or `continue` with a label;
  - recursion, once per method however often the method calls itself.
- Conditions, `switch` selectors and loop headers stay on the level of their statement; only bodies are nested.
- `try`, `finally`, `synchronized`, the bitwise operators `&` and `|`, and `break` or `continue` without a label do not count.
- Anonymous and local classes are left out, as for the other metrics.

A call is recursion when it has the name of the method and calls it without a scope (`find(n)`), on `this` (`this.find(n)`), on the class as `Own.this` (`Own.this.find(n)`) or on the class name (`Own.find(n)`), where `Own` is the simple name of the class that declares the method.
Overloads match by name, so `find(int)` calling `find(String)` counts.
A call on `super`, on an outer class (`Outer.this.find(n)` or `Outer.find(n)` from a nested class) or on any other object is not recursion.
Constructors are never recursive.

The breakdown reports the number of structural, hybrid and fundamental increments, the points added for nesting, and `recursion` as 1 when the method is recursive, which is also one of the fundamental increments.
The total is the sum of structural, hybrid, fundamental and nesting.
The risk level uses the bands of the cyclomatic complexity: low up to 5, medium up to 10, high up to 15, and very high above.

## Analysed Methods

The methods and constructors of top-level and nested classes, enums, records and interfaces are analysed.
Interface methods count only when they have a body, that is `default`, `static` and `private` methods.
Abstract and native methods of classes count with all metrics at zero except the line and argument counts.
The bodies of enum constants are anonymous classes and are left out.

A class is named by its fully qualified name, with nested classes separated by dots, such as `com.example.Outer.Inner`.
A method is named `com.example.Outer.Inner::method`, and a constructor is named after its class.
When a class declares several methods or constructors with the same name, all of them get their parameter types as written, without type arguments, such as `find(List,String...)`.
When the same method name occurs in several files, for example in the default package or in the same package of two source roots, it is reported for each file.

To leave a class or method out, put `@cca-ignore` into its Javadoc or into any comment between the previous declaration and its name.
Ignoring a class also ignores its nested classes.

### Limits

JavaParser 3.28.2, the latest release, cannot parse a local `enum`, that is an enum declared inside a method body (Java 16 and later).
A file that contains one is reported as `Parser failed for file <path>: ...` and skipped, and the analysis goes on with the other files.
Local classes, records and interfaces are parsed, and are left out as described above.

## Kotlin

Kotlin files (`.kt`) are analysed with the same metrics, thresholds, scores, reports, baselines and cache as Java files, so Java and Kotlin results can be compared directly.
Projects that mix both languages are analysed in one run, and each file is parsed by the parser of its language.
Kotlin is parsed with the PSI parser of the Kotlin compiler (`kotlin-compiler-embeddable`), without type resolution and without a classpath.
Kotlin scripts (`.kts`), such as `build.gradle.kts`, are not analysed.

### Analysed Kotlin functions

The functions of classes, data classes, enum classes, interfaces, objects and companion objects are analysed, and so are the top-level functions and extension functions of a file.

| Kotlin declaration                           | Reported as                                                                 |
|----------------------------------------------|-----------------------------------------------------------------------------|
| `fun f()` in `class Outer` of `package p`    | `p.Outer::f`                                                                |
| `fun f()` in `class Inner` nested in `Outer` | `p.Outer.Inner::f`                                                          |
| `fun f()` in `object Registry`               | `p.Registry::f`                                                             |
| `fun f()` in `companion object`              | `p.Outer.Companion::f`, or the name of a named companion object             |
| top-level `fun f()` in `Tools.kt`            | `p.ToolsKt::f`, the JVM facade class of the file                            |
| top-level function with `@file:JvmName("X")` | `p.X::f`                                                                    |
| top-level extension `fun String.f()`         | `p.ToolsKt::f`                                                              |
| extension function inside a class            | a function of that class                                                    |
| `constructor(...)` in `class Outer`          | `p.Outer::Outer`, like a Java constructor                                   |
| overloads `f(Int)` and `f(String?, vararg List<T>)` | `f(Int)` and `f(String,List...)`, the receiver of an extension first |

Interface functions count only when they have a body.
Abstract and external functions of classes count with all metrics at zero except the line and argument counts.
The primary constructor, `init` blocks, property getters and setters, and the bodies of enum entries are not analysed.
Anonymous objects (`object : Runnable { ... }`) and local classes are left out like Java anonymous and local classes.
Lambdas and local functions belong to the function that contains them.
The line count starts at the first annotation or modifier, not at the KDoc.
To leave out a class or function, put `@cca-ignore` into its KDoc, into a comment on the line it ends on, or into any comment between the previous declaration and its name.

### Java and Kotlin metric mapping

| Kotlin construct                                | Counts like the Java construct | Notes                                                                                     |
|-------------------------------------------------|--------------------------------|-------------------------------------------------------------------------------------------|
| `if` statement and `if` expression              | `if`                           | `else if` continues its chain like in Java.                                               |
| `when` with or without subject                  | `switch`                       | Cyclomatic: +1 per branch with conditions, the `else` branch adds nothing. Understandability: +1 per `when`. |
| `for (x in xs)`                                 | enhanced `for`                 |                                                                                           |
| `while`, `do`-`while`                           | `while`, `do`-`while`          |                                                                                           |
| `try`/`catch`                                   | `try`/`catch`                  | Each `catch` counts.                                                                      |
| `&&`, `\|\|`                                    | `&&`, `\|\|`                   |                                                                                           |
| elvis `a ?: b`                                  | no Java equivalent             | +1 cyclomatic only; no cognitive or understandability increment.                          |
| safe call `?.`, not-null assertion `!!`         | no Java equivalent             | Not counted, except as Halstead operators.                                                |
| lambda `{ x -> ... }`                           | lambda                         | Nests its body without an increment; its returns, variables and decisions belong to the function. |
| `return`, `return@label`                        | `return`                       | The value of an expression body (`fun f() = x`) is no return.                             |
| `val`, `var`, destructuring, lambda, catch, `for` and local function parameters | local variables | The implicit lambda parameter `it` counts once when it is used.               |
| `this.x`, `obj.x`, `obj?.x` and a bare property name | field access              | A bare name that is neither a parameter nor declared in the function is read as `this.x`. A receiver starting with an upper-case letter, such as `Companion.LIMIT`, is a type and does not count. |
| `Type(...)`                                     | `new Type(...)`                | Halstead operator `new:Type` for any call of an upper-case name.                          |
| `a to b` and other infix calls                  | method call                    | Halstead operator `call:to`.                                                              |
| `in`, `!in`, `is`, `!is`, `..`                  | `instanceof` and operators     | Halstead operators.                                                                       |
| string template `"a $b"`                        | string concatenation           | Halstead: each literal part and each name is an operand; a template without interpolation is one operand. |

The cyclomatic breakdown of a Kotlin function has the kinds `if`, `case`, `while`, `doWhile`, `forEach`, `catch`, `logicalAnd`, `logicalOr` and `elvis`.

### Kotlin coverage

The class names are the JVM names, so a JaCoCo, Cobertura or Clover report of Kotlin code matches without configuration.
A report names the facade class `pkg/ToolsKt` and the companion object `pkg/Outer$Companion`, which are `pkg.ToolsKt` and `pkg.Outer.Companion` here.
Kotlin compiles an `internal fun audit()` to `audit$<module>`, which matches `audit`.
A secondary constructor matches the `<init>` whose first line lies inside it, not the primary constructor.

### Kotlin limits

Names are not resolved, so the same guesses as for Java apply: a name starting with an upper-case letter is a type or constant, and a chain of at least two lower-case names before an upper-case name is a package.
A function called through a fully qualified name without an upper-case segment, such as `kotlin.math.max(a, b)`, reads `kotlin` as a property and `math` as a property access.
`this` in an extension function is its receiver and counts as `this`.
The Kotlin parser takes about half a second to start, once per run, and only when the analysed paths contain a Kotlin file.

## Result Interpretation

Interpreting the results of code complexity metrics like cognitive complexity, cyclomatic complexity, and other traditional measures requires understanding what each metric tells you about the code and how to use that information effectively.

Also consider that something that you perceive as easy or understandable might not be the same for someone else.

There are cases in which the code simply ends up with a lot of things that might result in a bad score but are necessary and acceptable under certain circumstances.
For example a complex calculation or a complex algorithm might have plenty of variables and input arguments.
If the problem **can't** be divided into smaller logical units (methods), the score for that class will inevitably be high.
