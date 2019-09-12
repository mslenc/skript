package skript.interop.wrappers

import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.values.*

class SkArrayWrapper<T>(val array: Array<T>, val elementCodec: SkCodec<T>, val env: SkriptEnv) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkArrayWrapperClassDef

    override fun getSize() = array.size

    override fun getValidSlot(index: Int): SkValue {
        return elementCodec.toSkript(array[index], env)
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = elementCodec.toKotlin(value, env)
    }
}

object SkArrayWrapperClassDef : SkClassDef("ArrayWrapper", SkAbstractListClassDef)

class SkCodecNativeArray<T>(val elementCodec: SkCodec<T>) : SkCodec<Array<T>> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return kotlinVal is Array<*> // TODO: check sub type somehow?
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): Array<T> {
        if (value is SkArrayWrapper<*>) {
            // TODO: check innards? for now we'll just assume it's right
            return value.array as Array<T>
        } else {
            TODO("Can't convert to native arrays yet")
        }
    }

    override fun toSkript(value: Array<T>, env: SkriptEnv): SkValue {
        return SkArrayWrapper(value, elementCodec, env)
    }
}