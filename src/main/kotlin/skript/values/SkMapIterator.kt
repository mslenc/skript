package skript.values

import skript.exec.RuntimeState
import skript.opcodes.SkIterator

// this type should only ever appear on the stack, used for implementing for-in loops
class SkMapIterator(val map: SkMap) : SkObject(), SkIterator {
    override val klass: SkClass
        get() = SkMapIteratorClass

    var pos = -1
    val entries: List<Pair<String, SkValue>> = ArrayList<Pair<String, SkValue>>().apply {
        map.props.forEach { key, value ->
            add(key to value)
        }
    }

    override fun moveToNext(): Boolean {
        return ++pos < entries.size
    }

    override fun getCurrentKey(): SkValue {
        return SkString(entries[pos].first)
    }

    override fun getCurrentValue(): SkValue {
        return entries[pos].second
    }
}

// and this class should never appear anywhere
object SkMapIteratorClass : SkClass("MapIterator", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        throw IllegalStateException("This should never be called")
    }
}