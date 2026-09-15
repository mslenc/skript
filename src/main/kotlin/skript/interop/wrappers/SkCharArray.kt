package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecChar
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkCharArray(override val nativeObj: CharArray) : SkAbstractNativeArray<CharArray>() {
    override val klass: SkClassDef
        get() = SkCharArrayClassDef

    override val elementCodec: SkCodec<*>
        get() = SkCodecChar

    override fun getSize() = nativeObj.size

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecChar.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkCharArrayIterator(nativeObj)
    }

    override fun unwrap(): CharArray {
        return nativeObj
    }
}

object SkCharArrayClassDef : SkClassDef("CharArray", SkAbstractListClassDef)

object SkCodecCharArray : SkCodecTypedArray<CharArray, Char>() {
    override val elementCodec: SkCodec<Char>
        get() = SkCodecChar

    override fun createArray(size: Int): CharArray {
        return CharArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is CharArray
    }

    override fun setElement(array: CharArray, index: Int, value: Char) {
        array[index] = value
    }

    override fun toSkript(value: CharArray, env: SkriptEnv) = SkCharArray(value)
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