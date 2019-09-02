package skript.util

class AstProps {
    private val values = HashMap<Key<*>, Any>()

    operator fun <E: Any> set(key: Key<E>, value: E) {
        values[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <E: Any> get(key: Key<E>): E? {
        return values[key] as E?
    }

    interface Key<E: Any>
}