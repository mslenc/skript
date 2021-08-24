package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecFloat
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkFloatArray(override val nativeObj: FloatArray) : SkAbstractNativeArray<FloatArray>() {
    override val klass: SkClassDef
        get() = SkFloatArrayClassDef

    override val elementCodec: SkCodec<*>
        get() = SkCodecFloat

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecFloat.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkFloatArrayIterator(nativeObj)
    }

    override fun unwrap(): FloatArray {
        return nativeObj
    }
}

object SkFloatArrayClassDef : SkClassDef("FloatArray", SkAbstractListClassDef)

object SkCodecFloatArray : SkCodecTypedArray<FloatArray, Float>() {
    override val elementCodec: SkCodec<Float>
        get() = SkCodecFloat

    override fun createArray(size: Int): FloatArray {
        return FloatArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is FloatArray
    }

    override fun setElement(array: FloatArray, index: Int, value: Float) {
        array[index] = value
    }

    override fun toSkript(value: FloatArray, env: SkriptEnv) = SkFloatArray(value)
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