package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecFloat
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkFloatArray(val array: FloatArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkFloatArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecFloat.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkFloatArrayIterator(array)
    }
}

object SkFloatArrayClassDef : SkClassDef("FloatArray", SkAbstractListClassDef)

object SkCodecFloatArray : SkCodec<FloatArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is FloatArray
    override fun toSkript(value: FloatArray, env: SkriptEnv) = SkFloatArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): FloatArray {
        if (value is SkFloatArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = FloatArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecFloat.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a FloatArray")
    }
}

class SkFloatArrayIterator(val array: FloatArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkFloatArrayIteratorClassDef

    var pos = -1

    override fun moveToNext(): Boolean {
        return ++pos < array.size
    }

    override fun getCurrentKey(): SkValue {
        return SkNumber.valueOf(pos)
    }

    override fun getCurrentValue(): SkValue {
        return array[pos].toSkript()
    }
}

object SkFloatArrayIteratorClassDef : SkClassDef("FloatArrayIterator", SkIteratorClassDef)