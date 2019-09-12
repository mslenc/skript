package skript.values

import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef

// this type should only ever appear on the stack, used for implementing for-in loops
class SkListIterator(val list: SkAbstractList) : SkIterator() {
    override val klass: SkClassDef
        get() = SkListIteratorClassDef

    var pos = -1
    val len = list.getSize() // we store the length, so that if the array grows or whatever, we don't loop forever; also,
                                    // to be consistent with map iteration (which would explode, if the map were modified while
                                    // iterating)

    override fun moveToNext(): Boolean {
        return ++pos < len
    }

    override fun getCurrentKey(): SkValue {
        return SkNumber.valueOf(pos)
    }

    override fun getCurrentValue(): SkValue {
        return list.getSlot(pos)
    }
}

object SkListIteratorClassDef : SkClassDef("ListIterator", SkIteratorClassDef)
