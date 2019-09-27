package skript.values

import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef

class SkMapIterator(val map: SkMap) : SkIterator() {
    override val klass: SkClassDef
        get() = SkMapIteratorClassDef

    var pos = -1
    val entriesCopy: List<Pair<String, SkValue>> = ArrayList<Pair<String, SkValue>>().apply {
        map.entries.forEach { (key, value) ->
            add(key to value)
        }
    }

    override fun moveToNext(): Boolean {
        return ++pos < entriesCopy.size
    }

    override fun getCurrentKey(): SkValue {
        return SkString(entriesCopy[pos].first)
    }

    override fun getCurrentValue(): SkValue {
        return entriesCopy[pos].second
    }
}

object SkMapIteratorClassDef : SkClassDef("MapIterator", SkIteratorClassDef)