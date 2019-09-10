package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.isString
import skript.notSupported
import skript.values.*

class SkArrayWrapper<T>(val array: Array<T>, val elementCodec: SkCodec<T>) : SkObject() {
    override val klass: SkClassDef
        get() = SkArrayWrapperClassDef

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        if (key.isString("size"))
            return SkNumber.valueOf(array.size)

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                return elementCodec.toSkript(array[index], state.env)
            }
        }

        return super.findMember(key, state)
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("size"))
            notSupported("Can't set size of native arrays")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                array[index] = elementCodec.toKotlin(key, state.env)
                return
            }
        }

        super.setMember(key, value, state)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        if (key.isString("size"))
            return true

        key.toNonNegativeIntOrNull()?.let { index ->
            return index < array.size
        }

        return super.hasOwnMember(key, state)
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't delete length of lists")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                notSupported("Can't delete elements of native arrays")
            }
        }

        super.deleteMember(key, state)
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(array.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert native arrays into numbers")
    }
}

object SkArrayWrapperClassDef : SkClassDef("ArrayWrapper", null)

class SkCodecNativeArray<T>(val elementCodec: SkCodec<T>) : SkCodec<Array<T>> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return kotlinVal is Array<*> // TODO: check sub type somehow?
    }

    override suspend fun toKotlin(value: SkValue, env: SkriptEnv): Array<T> {
        if (value is SkArrayWrapper<*>) {
            // TODO: do innards? for now we'll just assume it's right
            return value.array as Array<T>
        } else {
            TODO("Can't convert to native arrays yet")
        }
    }

    override suspend fun toSkript(value: Array<T>, env: SkriptEnv): SkValue {
        return SkArrayWrapper(value, elementCodec)
    }
}