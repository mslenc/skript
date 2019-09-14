package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecBoolean
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkBooleanArray(override val nativeObj: BooleanArray) : SkAbstractNativeArray<BooleanArray>() {
    override val klass: SkClassDef
        get() = SkBooleanArrayClassDef

    override val elementCodec: SkCodecBoolean
        get() = SkCodecBoolean

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecBoolean.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkBooleanArrayIterator(nativeObj)
    }
}

object SkBooleanArrayClassDef : SkClassDef("BooleanArray", SkAbstractListClassDef)

object SkCodecBooleanArray : SkCodecTypedArray<BooleanArray, Boolean>() {
    override val elementCodec: SkCodec<Boolean>
        get() = SkCodecBoolean

    override fun createArray(size: Int): BooleanArray {
        return BooleanArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is BooleanArray
    }

    override fun setElement(array: BooleanArray, index: Int, value: Boolean) {
        array[index] = value
    }

    override fun toSkript(value: BooleanArray, env: SkriptEnv) = SkBooleanArray(value)
}

class SkBooleanArrayIterator(val array: BooleanArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkBooleanArrayIteratorClassDef

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

object SkBooleanArrayIteratorClassDef : SkClassDef("BooleanArrayIterator", SkIteratorClassDef)