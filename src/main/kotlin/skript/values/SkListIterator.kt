package skript.values

import skript.exec.RuntimeState
import skript.opcodes.SkIterator

// this type should only ever appear on the stack, used for implementing for-in loops
class SkListIterator(val list: SkList) : SkObject(), SkIterator {
    override val klass: SkClass
        get() = SkListIteratorClass

    var pos = -1
    val len = list.getLength() // we store the length, so that if the array grows or whatever, we don't loop forever; also,
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

// and this class should never appear anywhere
object SkListIteratorClass : SkClass("ListIterator", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        throw IllegalStateException("This should never be called")
    }
}