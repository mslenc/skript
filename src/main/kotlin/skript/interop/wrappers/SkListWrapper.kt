package skript.interop.wrappers

import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.values.*

class SkListWrapper<T>(val list: List<T>, val elementCodec: SkCodec<T>, val env: SkriptEnv) : SkAbstractList() {
    override val klass: SkClassDef
        get() = SkListWrapperClassDef

    override fun getSize(): Int {
        return list.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return elementCodec.toSkript(list[index], env)
    }
}

object SkListWrapperClassDef : SkClassDef("ListWrapper", null)

class SkCodecNativeList<T>(val elementCodec: SkCodec<T>) : SkCodec<List<T>> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return kotlinVal is List<*> // TODO: check sub type somehow?
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): List<T> {
        if (value is SkListWrapper<*>) {
            // TODO: do innards? for now we'll just assume it's right
            return value.list as List<T>
        } else {
            TODO("Can't convert to native lists yet")
        }
    }

    override fun toSkript(value: List<T>, env: SkriptEnv): SkValue {
        return SkListWrapper(value, elementCodec, env)
    }
}