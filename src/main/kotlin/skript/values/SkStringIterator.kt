package skript.values

import skript.exec.RuntimeState
import skript.opcodes.SkIterator

// this type should only ever appear on the stack, used for implementing for-in loops
class SkStringIterator(value: SkString) : SkObject(), SkIterator {
    override val klass: SkClass
        get() = SkStringIteratorClass

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
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        throw IllegalStateException("This should never be called")
    }
}