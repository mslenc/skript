package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecInt
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkIntArray(override val nativeObj: IntArray) : SkAbstractNativeArray<IntArray>() {
    override val klass: SkClassDef
        get() = SkIntArrayClassDef

    override val elementCodec: SkCodec<*>
        get() = SkCodecInt

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecInt.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkIntArrayIterator(nativeObj)
    }

    override fun unwrap(): IntArray {
        return nativeObj
    }
}

object SkIntArrayClassDef : SkClassDef("IntArray", SkAbstractListClassDef)

object SkCodecIntArray : SkCodecTypedArray<IntArray, Int>() {
    override val elementCodec: SkCodec<Int>
        get() = SkCodecInt

    override fun createArray(size: Int): IntArray {
        return IntArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is IntArray
    }

    override fun setElement(array: IntArray, index: Int, value: Int) {
        array[index] = value
    }

    override fun toSkript(value: IntArray, env: SkriptEnv) = SkIntArray(value)
}

class SkIntArrayIterator(val array: IntArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkIntArrayIteratorClassDef

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

object SkIntArrayIteratorClassDef : SkClassDef("IntArrayIterator", SkIteratorClassDef)