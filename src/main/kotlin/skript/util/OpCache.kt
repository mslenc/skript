package skript.util

class OpCache<T>(private val cache: Array<T>, private val factory: (Int)->T) {
    val last = cache.size - 1

    operator fun invoke(value: Int): T {
        return if (value in 0..last) {
            cache[value]
        } else {
            factory(value)
        }
    }

    companion object {
        inline fun <reified T> createOpCache(size: Int, noinline factory: (Int)->T): OpCache<T> {
            val cache = Array(size) { factory(it) }
            return OpCache(cache, factory)
        }
    }
}

