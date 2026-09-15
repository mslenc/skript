package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecShort
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkShortArray(override val nativeObj: ShortArray) : SkAbstractNativeArray<ShortArray>() {
    override val klass: SkClassDef
        get() = SkShortArrayClassDef

    override val elementCodec: SkCodecShort
        get() = SkCodecShort

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecShort.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkShortArrayIterator(nativeObj)
    }

    override fun unwrap(): ShortArray {
        return nativeObj
    }
}

object SkShortArrayClassDef : SkClassDef("ShortArray", SkAbstractListClassDef)

object SkCodecShortArray : SkCodecTypedArray<ShortArray, Short>() {
    override val elementCodec: SkCodec<Short>
        get() = SkCodecShort

    override fun createArray(size: Int): ShortArray {
        return ShortArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is ShortArray
    }

    override fun setElement(array: ShortArray, index: Int, value: Short) {
        array[index] = value
    }

    override fun toSkript(value: ShortArray, env: SkriptEnv) = SkShortArray(value)
}

class SkShortArrayIterator(val array: ShortArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkShortArrayIteratorClassDef

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

object SkShortArrayIteratorClassDef : SkClassDef("ShortArrayIterator", SkIteratorClassDef)