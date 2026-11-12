package fixtures.kotlin

class IfStatements {

    fun chain(a: Int): Int {
        if (a > 3) {
            return 3
        } else if (a > 2) {
            return 2
        } else if (a > 1) {
            return 1
        } else {
            return 0
        }
    }

    fun ifExpression(a: Int): String {
        val sign = if (a > 0) "positive" else if (a < 0) "negative" else "zero"
        return sign
    }

    fun nestedInThenAndElse(a: Boolean, b: Boolean, c: Boolean) {
        if (a) {
            if (b) {
                if (c) {
                    log("abc")
                }
            }
        } else {
            if (b) log("b") else log("none")
        }
    }

    fun braceElseIf(a: Int) {
        if (a == 1) {
            log("1")
        } else {
            if (a == 2) {
                log("2")
            }
        }
    }

    fun ifInsideLambdaInsideIf(values: List<Int>) {
        if (values.isNotEmpty()) {
            values.forEach {
                if (it > 0) log("positive")
            }
        }
    }

    fun ifsInLoopsWhenAndTry(values: List<Int>) {
        for (value in values) {
            if (value > 0) log("positive")
        }
        when (values.size) {
            0 -> if (values.isEmpty()) log("empty")
            else -> log("some")
        }
        try {
            if (values.contains(1)) log("one")
        } catch (e: IllegalStateException) {
            if (e.message == null) log("no message")
        }
    }

    fun log(message: String) = println(message)
}
