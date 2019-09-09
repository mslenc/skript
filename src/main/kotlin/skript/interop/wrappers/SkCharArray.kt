package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.interop.SkCodecChar
import skript.io.toSkript
import skript.isString
import skript.notSupported
import skript.opcodes.SkIterator
import skript.values.*

class SkCharArray(val array: CharArray) : SkObject() {
    override val klass: SkClassDef
        get() = SkCharArrayClassDef

    override fun getKind(): SkValueKind {
        return SkValueKind.OBJECT
    }

    override fun asObject(): SkObject {
        return this
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(array.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert a CharArray into a number")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't change length of a CharArray")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                array[index] = SkCodecChar.toKotlin(key, state)
                return
            }
        }

        super.setMember(key, value, state)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        if (key.isString("length"))
            return true

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size)
                return true
        }

        return super.hasOwnMember(key, state)
    }

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(array.size)

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                return array[index].toSkript()
            }
        }

        return super.findMember(key, state)
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't delete length of CharArrays")

        super.deleteMember(key, state)
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        notSupported("CharArrays can't be used to make ranges")
    }

    override suspend fun makeIterator(): SkValue {
        return SkCharArrayIterator(array)
    }
}

object SkCharArrayClassDef : SkClassDef("CharArray", null)

object SkCodecCharArray : SkCodec<CharArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is CharArray
    override suspend fun toSkript(value: CharArray, state: RuntimeState) = SkCharArray(value)
    override suspend fun toKotlin(value: SkValue, state: RuntimeState): CharArray {
        if (value is SkCharArray)
            return value.array

        if (value is SkList) {
            val len = value.getLength()
            val result = CharArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecChar.toKotlin(value.elements[i] ?: SkNull, state)
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a CharArray")
    }
}

class SkCharArrayIterator(val array: CharArray) : SkObject(), SkIterator {
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

object SkCharArrayIteratorClassDef : SkClassDef("CharArrayIterator", null)