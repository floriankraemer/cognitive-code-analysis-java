package fixtures.kotlin

class Operations(private val count: Int) {

    fun empty() {}

    fun add(a: Int, b: Int) = a + b

    fun assignmentsAndUnary(a: Int): Int {
        var x = a
        x += 1
        x = -x
        x++
        return x
    }

    fun chainedCalls(s: String) = s.trim().length

    fun creation(n: Int) = StringBuilder(n)

    fun nullSafety(text: String?) = text?.length ?: text!!.hashCode()

    fun typeChecksAndRanges(o: Any, i: Int) = o is String && i in 1..9 && o !is Int

    fun literals() = listOf("s", 'c', 1, 2L, 1.5, true, null)

    fun templates(name: String) = "hello $name and ${name.length}!"

    fun lambdasAndReferences(list: List<Int>) = list.map { x -> x * 2 }.forEach(::println)

    fun infixCall(a: Int) = a to count

    fun whenConditions(o: Any) = when (o) {
        is String -> 1
        in 1..2 -> 2
        else -> 3
    }
}
