package skript.values

import skript.opcodes.SkIterator

// this type should only ever appear on the stack, used for implementing for-in loops
class SkStringIterator(value: SkString) : SkObject(SkStringIteratorClass), SkIterator {
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

// and this class should never appear anywhere
object SkStringIteratorClass : SkClass("StringIterator", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        throw IllegalStateException("This should never be called")
    }
}