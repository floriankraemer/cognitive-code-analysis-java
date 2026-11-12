package fixtures.kotlin

class Returns {

    fun returnsInLambdas(values: List<Int>): Int {
        values.forEach { value ->
            if (value < 0) return@forEach
            println(value)
        }
        val first = values.firstOrNull { return@firstOrNull it > 0 }
        return first ?: 0
    }

    fun expressionBodyIsNoReturn(value: Int) = value * 2

    fun anonymousObjectIsSkipped(value: Int): Runnable {
        val runnable = object : Runnable {
            override fun run() {
                if (value > 0) {
                    return
                }
                val hidden = value
            }
        }
        return runnable
    }

    fun localClassIsSkipped(value: Int): Int {
        class Local {
            fun twice(): Int {
                if (value > 0) return value * 2
                return 0
            }
        }
        return Local().twice()
    }

    fun localFunctionBelongsToTheFunction(value: Int): Int {
        fun clamp(limit: Int): Int {
            if (value > limit) return limit
            return value
        }
        return clamp(10)
    }
}
