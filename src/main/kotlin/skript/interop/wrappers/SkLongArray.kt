package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecLong
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkLongArray(override val nativeObj: LongArray) : SkAbstractNativeArray<LongArray>() {
    override val klass: SkClassDef
        get() = SkLongArrayClassDef

    override val elementCodec: SkCodec<*>
        get() = SkCodecLong

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecLong.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkLongArrayIterator(nativeObj)
    }
}

object SkLongArrayClassDef : SkClassDef("LongArray", SkAbstractListClassDef)

object SkCodecLongArray : SkCodecTypedArray<LongArray, Long>() {
    override val elementCodec: SkCodec<Long>
        get() = SkCodecLong

    override fun createArray(size: Int): LongArray {
        return LongArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is LongArray
    }

    override fun setElement(array: LongArray, index: Int, value: Long) {
        array[index] = value
    }

    override fun toSkript(value: LongArray, env: SkriptEnv) = SkLongArray(value)
}

class SkLongArrayIterator(val array: LongArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkLongArrayIteratorClassDef

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

object SkLongArrayIteratorClassDef : SkClassDef("LongArrayIterator", SkIteratorClassDef)