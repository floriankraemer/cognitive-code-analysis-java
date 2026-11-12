# Cognitive Complexity Analysis

## Why bother?

Easy to read and understand code has a huge impact on the business. It can reduce the time it takes to onboard new developers, make it easier to maintain and extend the codebase, and reduce the likelihood of bugs and errors.

* **Affordability**: Developers spend **8 to 10 times** more time reading code than writing it. This means that the majority of a developer's time is spent trying to understand and reason about code. By focusing on cognitive complexity, we can make this process easier and more efficient.
* **Security**: Code that is hard to understand is more likely to contain bugs and errors. By reducing cognitive complexity, we can make code more reliable and easier to test.
* **Learnability**: New developers often struggle to understand complex codebases. By reducing cognitive complexity, we can make it easier for new developers to get up to speed and start contributing quickly.

## What is the difference to Cyclomatic Complexity?

**Cyclomatic complexity**, introduced by Thomas McCabe in 1976, measures the number of linearly independent paths through a program. It's based on the control flow graph of the code, where each decision point (like if, while, for, etc.) increases the complexity.

**Cognitive complexity**, is a more recent metric designed to better align with how human brains process code. It penalizes structures that are difficult for humans to understand, focusing on the mental effort required to read and understand code.

**Cognitive complexity measures how hard it is for a human to understand the code, while cyclomatic complexity measures how hard your code is to test.**

## How is Cognitive Complexity calculated?

A score is calculated that increases logarithmically as the input value exceeds a specified threshold. The result is 0 when the value is less than or equal to the threshold. When the value is greater than the threshold, the weight is calculated using a logarithmic function that controls the rate of increase.

Given a value of 75 that is greater than the threshold of 50, the function calculates the logarithmic weight as log(1 + (75 - 50) / 10, M_E) which results in approximately 2.302585.

This calculation is done for each metric and the results are summed to get the final cognitive complexity score for a method.

## Metrics Collected

All counts are per method or constructor.
Lambdas belong to the method that contains them.
Anonymous and local classes are left out, both as units of their own and from the counts of the enclosing method.

- **Line Count**:
  - **Description**: The number of lines from the first annotation or modifier of the declaration to its closing brace. Javadoc is not counted.
  - **Purpose**: Measures the size of the method in terms of lines of code, which can give an indication of its complexity and readability.

- **Argument Count**:
  - **Description**: The number of parameters of the method. A compact record constructor has the record components as its parameters.
  - **Purpose**: High argument counts can indicate that a method is doing too much or is highly dependent on external inputs, which may be a sign that the method needs refactoring.

- **Return Count**:
  - **Description**: The number of `return` statements, including those inside lambdas.
  - **Purpose**: Multiple return points can make a method harder to follow and maintain. This metric helps identify methods that may benefit from being refactored for clarity.

- **Variable Count**:
  - **Description**: The number of distinct variable names declared in the method body, without the method parameters, plus `this`.
    Local variables, `for` and for-each variables, lambda and catch parameters, pattern variables and `try` resources count.
    A name that is reused, for example in two loops, counts once; the unnamed variable `_` does not count.
    `this` counts once when the method reads an instance member, either as `this.x` or as a bare field name (see Property Call Count).
  - **Purpose**: A high variable count can indicate a method that is doing too much or is overly complex. Monitoring this metric helps to ensure methods remain simple and focused.

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
  - **Description**: The number of `if` statements. An `else if` continues its chain and does not count. A conditional expression (`a ? b : c`) does not count.
  - **Purpose**: Like else count, this measures the complexity of branching logic. A high if count can make code harder to understand and maintain.

- **If Nesting Level**:
  - **Description**: The maximum depth of `if` statements nested in the branches of other `if` statements. An `else if` stays on the depth of its chain. Loops, `try` and `switch` do not add depth.
  - **Purpose**: Deeply nested `if` statements can be difficult to follow and may indicate a need for refactoring into smaller, more manageable pieces.

- **Else Count**:
  - **Description**: The number of `else` and `else if` branches.
  - **Purpose**: Helps measure the complexity of branching logic. A high else count might indicate complex or tightly coupled conditional logic.

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

## Result Interpretation

Interpreting the results of code complexity metrics like cognitive complexity, cyclomatic complexity, and other traditional measures requires understanding what each metric tells you about the code and how to use that information effectively.

Also consider that something that you perceive as easy or understandable might not be the same for someone else.

There are cases in which the code simply ends up with a lot of things that might result in a bad score but are necessary and acceptable under certain circumstances. For example a complex calculation or a complex algorithm might have plenty of variables and input arguments. If the problem **can't** be divided into smaller logical units (methods), the score for that class will inevitably be high.
