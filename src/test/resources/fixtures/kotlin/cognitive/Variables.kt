package fixtures.kotlin

class Variables(private val count: Int, private val other: Variables?) {

    private var total = 0

    fun distinctNames(a: Int): Int {
        val x = a + 1
        var y = x * 2
        y += 1
        val x2 = y
        for (i in 0 until 3) {
            println(i)
        }
        for (i in 0 until 2) {
            println(i)
        }
        return x2
    }

    fun destructuringLambdaCatchAndForVariables(map: Map<String, Int>) {
        val (first, second) = Pair(1, 2)
        for ((key, value) in map) {
            println(key + value)
        }
        map.forEach { (k, _) -> println(k) }
        listOf(1).map { it * first + second }
        try {
            println(map)
        } catch (e: Exception) {
            println(e)
        }
    }

    fun propertyAccess(holder: Variables): Int {
        val local = holder.total + this.total
        return local + other?.count!! + Companion.LIMIT + holder.other!!.total
    }

    fun implicitThis(factor: Int): Int {
        total += factor
        return count * total
    }

    fun shadowsProperty(): Int {
        val count = 2
        return count + total
    }

    fun onlyParametersAndConstants(value: Int): Int {
        val doubled = value * LIMIT
        return java.lang.Math.max(doubled, Int.MAX_VALUE)
    }

    fun itWithoutLambdaIsAProperty(): Int = it

    val it: Int = 0

    companion object {
        const val LIMIT = 3
    }
}
