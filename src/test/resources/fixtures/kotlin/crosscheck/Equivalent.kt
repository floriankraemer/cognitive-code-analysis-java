package fixtures.crosscheck

import java.util.List

class Equivalent {

  var count = 0

  fun chain(a: Int): Int {
    if (a > 3) {
      return 3
    } else if (a > 2) {
      return 2
    } else {
      return 0
    }
  }

  fun nesting(values: List<Int>, flag: Boolean) {
    for (value in values) {
      if (value > 0) {
        if (flag) {
          log(value)
        } else {
          log(0)
        }
      }
    }
  }

  fun loops(values: List<Int>) {
    var i = 0
    while (i < 10) {
      i++
    }
    do {
      i--
    } while (i > 0)
    for (value in values) {
      log(value)
    }
  }

  fun switchLikeWhen(a: Int) {
    when (a) {
      1, 2 -> log(1)
      3 -> {
        if (a > 0) {
          log(3)
        }
      }
      else -> log(0)
    }
  }

  fun tryCatch() {
    try {
      work()
    } catch (e: IllegalStateException) {
      log(e)
    } catch (e: RuntimeException) {
      log(e)
    }
  }

  fun lambdas(values: List<Int>) {
    values.forEach { value ->
      if (value > 0) {
        log(value)
      }
    }
  }

  fun logical(a: Boolean, b: Boolean, c: Boolean): Boolean {
    return a && b || c && !a
  }

  fun properties(other: Equivalent): Int {
    val sum = this.count + other.count
    return sum + count
  }

  fun recursive(n: Int): Int {
    if (n <= 1) {
      return 1
    }
    return n * recursive(n - 1)
  }

  fun labeled(rows: List<List<Int>>) {
    outer@
    for (row in rows) {
      for (cell in row) {
        if (cell < 0) {
          continue@outer
        }
      }
    }
  }

  fun work() {
  }

  fun log(value: Any) {
  }
}
