package fixtures.kotlin

class Understandability {

    fun nesting(values: List<Int>) {
        for (value in values) {
            if (value > 0) {
                while (value > 10) {
                    println(value)
                }
            }
        }
    }

    fun whenCountsOnce(a: Int, b: Boolean) {
        when (a) {
            1 -> if (b) println("one")
            2 -> println("two")
            else -> println("other")
        }
    }

    fun labeledJumps(rows: List<List<Int>>) {
        outer@ for (row in rows) {
            for (cell in row) {
                if (cell < 0) continue@outer
                if (cell > 9) break@outer
                if (cell == 5) break
            }
        }
    }

    fun lambdaNests(values: List<Int>) {
        values.forEach {
            if (it > 0) println(it)
        }
    }

    fun localFunctionNests(value: Int): Int {
        fun twice(): Int {
            if (value > 0) return value * 2
            return 0
        }
        return twice()
    }

    fun sequences(a: Boolean, b: Boolean, c: Boolean, d: Boolean): Boolean {
        return a && b && (c || d) || !(a && d)
    }

    fun elvisAddsNothing(text: String?): Int = text?.length ?: 0

    fun factorial(n: Int): Int = if (n <= 1) 1 else n * factorial(n - 1)

    fun recursionOnThis(n: Int): Int {
        return if (n == 0) 0 else this.recursionOnThis(n - 1) + this@Understandability.recursionOnThis(0)
    }

    fun noRecursionOnOtherReceiver(other: Understandability) {
        other.noRecursionOnOtherReceiver(this)
    }

    fun tryCatch() {
        try {
            println()
        } catch (e: Exception) {
            if (e.message != null) println(e)
        }
    }

    fun doWhileLoop(values: MutableList<Int>) {
        do {
            values.removeLast()
        } while (values.isNotEmpty() && values.size > 2)
    }
}
