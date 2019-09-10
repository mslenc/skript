package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.interop.SkCodecFloat
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.isString
import skript.notSupported
import skript.opcodes.SkIterator
import skript.values.*

class SkFloatArray(val array: FloatArray) : SkObject() {
    override val klass: SkClassDef
        get() = SkFloatArrayClassDef

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
        notSupported("Can't convert a FloatArray into a number")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't change length of a FloatArray")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                array[index] = SkCodecFloat.toKotlin(key, state.env)
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
            notSupported("Can't delete length of FloatArrays")

        super.deleteMember(key, state)
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        notSupported("FloatArrays can't be used to make ranges")
    }

    override suspend fun makeIterator(): SkValue {
        return SkFloatArrayIterator(array)
    }
}

object SkFloatArrayClassDef : SkClassDef("FloatArray", null)

object SkCodecFloatArray : SkCodec<FloatArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is FloatArray
    override suspend fun toSkript(value: FloatArray, env: SkriptEnv) = SkFloatArray(value)
    override suspend fun toKotlin(value: SkValue, env: SkriptEnv): FloatArray {
        if (value is SkFloatArray)
            return value.array

        if (value is SkList) {
            val len = value.getLength()
            val result = FloatArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecFloat.toKotlin(value.elements[i] ?: SkNull, env)
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a FloatArray")
    }
}

class SkFloatArrayIterator(val array: FloatArray) : SkObject(), SkIterator {
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

object SkFloatArrayIteratorClassDef : SkClassDef("FloatArrayIterator", null)