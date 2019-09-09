package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.isString
import skript.notSupported
import skript.values.*

class SkListWrapper<T>(val list: List<T>, val elementCodec: SkCodec<T>) : SkObject() {
    override val klass: SkClassDef
        get() = SkListWrapperClassDef

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(list.size)

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < list.size) {
                return elementCodec.toSkript(list[index], state)
            }
        }

        return super.findMember(key, state)
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't set length of read-only lists")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < list.size) {
                notSupported("Can't set elements of read-only lists")
            }
        }

        super.setMember(key, value, state)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        if (key.isString("length"))
            return true

        key.toNonNegativeIntOrNull()?.let { index ->
            return index < list.size
        }

        return super.hasOwnMember(key, state)
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't delete length of lists")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < list.size) {
                notSupported("Can't delete elements of read-only lists")
            }
        }

        super.deleteMember(key, state)
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(list.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert lists into numbers")
    }
}

object SkListWrapperClassDef : SkClassDef("ListWrapper", null)