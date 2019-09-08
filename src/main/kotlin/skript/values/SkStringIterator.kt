package skript.values

import skript.opcodes.SkIterator

// this type should only ever appear on the stack, used for implementing for-in loops
class SkStringIterator(value: SkString) : SkObject(), SkIterator {
    override val klass: SkClassDef
        get() = SkStringIteratorClassDef

    var index = -1
    val string = value.value

    override fun moveToNext(): Boolean {
        return ++index < string.length
    }

    override fun getCurrentKey(): SkValue {
        return SkNumber.valueOf(index)
    }

    override fun getCurrentValue(): SkValue {
        return SkString(string[index].toString())
    }
}

object SkStringIteratorClassDef : SkClassDef("StringIterator", SkObjectClassDef)