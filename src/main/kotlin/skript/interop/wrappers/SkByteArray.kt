package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecByte
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkByteArray(val array: ByteArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkByteArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecByte.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkByteArrayIterator(array)
    }
}

object SkByteArrayClassDef : SkClassDef("ByteArray", null)

object SkCodecByteArray : SkCodec<ByteArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is ByteArray
    override fun toSkript(value: ByteArray, env: SkriptEnv) = SkByteArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): ByteArray {
        if (value is SkByteArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = ByteArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecByte.toKotlin(value)
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a ByteArray")
    }
}

class SkByteArrayIterator(val array: ByteArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkByteArrayIteratorClassDef

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

object SkByteArrayIteratorClassDef : SkClassDef("ByteArrayIterator", SkIteratorClassDef)