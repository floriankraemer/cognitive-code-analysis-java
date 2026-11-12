package fixtures.kotlin

class Decisions {

    fun empty() {}

    fun elseIfChain(a: Int) {
        if (a > 0) {} else if (a < 0) {} else {}
    }

    fun whenWithSubject(a: Int): String = when (a) {
        1, 2, 3 -> "small"
        in 4..9 -> "medium"
        is Int -> "int"
        else -> "large"
    }

    fun whenWithoutSubject(a: Int): String {
        return when {
            a < 0 -> "negative"
            a == 0 -> "zero"
            else -> "positive"
        }
    }

    fun loops(list: List<String>) {
        while (list.isEmpty()) {}
        do {} while (list.isEmpty())
        for (s in list) {}
    }

    fun tryCatches() {
        try {
            println()
        } catch (e: IllegalStateException) {
        } catch (e: RuntimeException) {
        } finally {
        }
    }

    fun logical(a: Boolean, b: Boolean, c: Boolean): Boolean = a && b || c && !a

    fun nullHandling(text: String?): Int {
        val length = text?.length ?: 0
        return length + text!!.length
    }

    fun lambdasAddNothing(list: List<Int>) = list.filter { it > 0 }.map { it * 2 }
}
