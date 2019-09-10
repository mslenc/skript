package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.interop.SkCodecShort
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.isString
import skript.notSupported
import skript.opcodes.SkIterator
import skript.values.*

class SkShortArray(val array: ShortArray) : SkObject() {
    override val klass: SkClassDef
        get() = SkShortArrayClassDef

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
        notSupported("Can't convert a ShortArray into a number")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't change length of a ShortArray")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                array[index] = SkCodecShort.toKotlin(key, state.env)
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
            notSupported("Can't delete length of ShortArrays")

        super.deleteMember(key, state)
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        notSupported("ShortArrays can't be used to make ranges")
    }

    override suspend fun makeIterator(): SkValue {
        return SkShortArrayIterator(array)
    }
}

object SkShortArrayClassDef : SkClassDef("ShortArray", null)

object SkCodecShortArray : SkCodec<ShortArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is ShortArray
    override suspend fun toSkript(value: ShortArray, env: SkriptEnv) = SkShortArray(value)
    override suspend fun toKotlin(value: SkValue, env: SkriptEnv): ShortArray {
        if (value is SkShortArray)
            return value.array

        if (value is SkList) {
            val len = value.getLength()
            val result = ShortArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecShort.toKotlin(value.elements[i] ?: SkNull, env)
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a ShortArray")
    }
}

class SkShortArrayIterator(val array: ShortArray) : SkObject(), SkIterator {
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

object SkShortArrayIteratorClassDef : SkClassDef("ShortArrayIterator", null)