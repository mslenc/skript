package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.HoldsNative
import skript.interop.SkCodec
import skript.notSupported
import skript.typeError
import skript.values.*

abstract class SkAbstractNativeArray<ARR: Any> : SkAbstractList(), HoldsNative<ARR> {
    abstract val elementCodec: SkCodec<*>

    protected abstract fun setValidSlot(index: Int, value: SkValue)

    final override fun setSlot(index: Int, value: SkValue) {
        if (index < 0 || index >= getSize())
            typeError("With native arrays, can't set elements outside of pre-defined size")

        setValidSlot(index, value)
    }

    override fun setSize(newSize: Int) {
        typeError("With native arrays, size can't be changed")
    }

    override fun add(value: SkValue) {
        typeError("With native arrays, can't set elements outside of pre-defined size")
    }

    override fun addAll(values: List<SkValue>) {
        typeError("With native arrays, can't set elements outside of pre-defined size")
    }

    override fun removeLast(): SkValue {
        typeError("With native arrays, size can't be changed")
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert native arrays into numbers")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        notSupported("Native arrays can't be used to make ranges")
    }
}