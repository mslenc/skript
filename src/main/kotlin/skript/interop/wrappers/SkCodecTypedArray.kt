package skript.interop.wrappers

import skript.interop.ConversionType
import skript.interop.HoldsNative
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.typeError
import skript.values.SkAbstractList
import skript.values.SkValue

abstract class SkCodecTypedArray<ARR: Any, EL: Any> : SkCodec<ARR> {
    abstract val elementCodec: SkCodec<EL>
    abstract fun createArray(size: Int): ARR
    abstract fun isArrayInstance(obj: Any): Boolean
    abstract fun setElement(array: ARR, index: Int, value: EL)

    override fun canConvert(value: SkValue): ConversionType {
        if (value is HoldsNative<*> && isArrayInstance(value.nativeObj))
            return ConversionType.EXACT

        if (value is SkAbstractList) {
            for (i in 0 until value.getSize()) {
                if (elementCodec.canConvert(value.getSlot(i)) == ConversionType.NOT_POSSIBLE) {
                    return ConversionType.NOT_POSSIBLE
                }
            }
            return ConversionType.COERCE
        }

        return ConversionType.NOT_POSSIBLE
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): ARR {
        if (value is HoldsNative<*> && isArrayInstance(value.nativeObj))
            return value.nativeObj as ARR

        if (value is SkAbstractList) {
            val array = createArray(value.getSize())
            for (i in 0 until value.getSize())
                setElement(array, i, elementCodec.toKotlin(value.getSlot(i), env))
            return array
        }

        typeError("Can't convert to ${ createArray(0).javaClass }")
    }
}