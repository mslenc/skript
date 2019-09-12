package skript.values

import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef

class SkMapIterator(val map: SkMap) : SkIterator() {
    override val klass: SkClassDef
        get() = SkMapIteratorClassDef

    var pos = -1
    val entries: List<Pair<String, SkValue>> = ArrayList<Pair<String, SkValue>>().apply {
        map.elements.forEach { (key, value) ->
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

object SkMapIteratorClassDef : SkClassDef("MapIterator", SkIteratorClassDef)