package skript.values

// the vast majority of objects is expected to not have custom properties, so we cater to that case instead of constructing
// a bazillion empty maps
abstract class Props {
    abstract val size: Int

    abstract fun withAdded(key: String, value: SkValue): Props
    abstract fun withRemoved(key: String): Props
    abstract fun get(key: String): SkValue?
    abstract fun extractInto(map: MutableMap<String, SkValue>)
    abstract fun forEach(block: (String, SkValue)->Unit)
}

object EmptyProps : Props() {
    override fun withAdded(key: String, value: SkValue): Props {
        return SingleProp(key, value)
    }

    override fun withRemoved(key: String): Props {
        return this
    }

    override fun get(key: String): SkValue? {
        return null
    }

    override fun extractInto(map: MutableMap<String, SkValue>) {
        // nothing to do
    }

    override fun forEach(block: (String, SkValue) -> Unit) {
        // nothing to do
    }

    override val size: Int
        get() = 0
}

class SingleProp(val key: String, var value: SkValue) : Props() {
    override fun withAdded(key: String, value: SkValue): Props {
        return when (key) {
            this.key -> {
                this.value = value
                this
            }
            else -> {
                val fullProps = FullProps()
                fullProps.values[this.key] = this.value
                fullProps.values[key] = value
                fullProps
            }
        }
    }

    override fun withRemoved(key: String): Props {
        return when (key) {
            this.key -> EmptyProps
            else -> this
        }
    }

    override fun get(key: String): SkValue? {
        return if (key == this.key) value else null
    }

    override fun extractInto(map: MutableMap<String, SkValue>) {
        map[key] = value
    }

    override fun forEach(block: (String, SkValue) -> Unit) {
        block(key, value)
    }

    override val size: Int
        get() = 0
}

class FullProps: Props() {
    val values = LinkedHashMap<String, SkValue>()

    override fun withAdded(key: String, value: SkValue): Props {
        values[key] = value
        return this
    }

    override fun withRemoved(key: String): Props {
        values.remove(key)

        return when {
            values.isEmpty() -> EmptyProps
            else -> this
        }
    }

    override fun get(key: String): SkValue? {
        return values[key]
    }

    override fun extractInto(map: MutableMap<String, SkValue>) {
        map.putAll(values)
    }

    override fun forEach(block: (String, SkValue) -> Unit) {
        values.forEach(block)
    }

    override val size: Int
        get() = values.size
}