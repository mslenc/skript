package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecChar
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkCharArray(val array: CharArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkCharArrayClassDef

    override fun getSize() = array.size

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecChar.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkCharArrayIterator(array)
    }
}

object SkCharArrayClassDef : SkClassDef("CharArray", null)

object SkCodecCharArray : SkCodec<CharArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is CharArray
    override fun toSkript(value: CharArray, env: SkriptEnv) = SkCharArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): CharArray {
        if (value is SkCharArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = CharArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecChar.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a CharArray")
    }
}

class SkCharArrayIterator(val array: CharArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkCharArrayIteratorClassDef

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

object SkCharArrayIteratorClassDef : SkClassDef("CharArrayIterator", SkIteratorClassDef)