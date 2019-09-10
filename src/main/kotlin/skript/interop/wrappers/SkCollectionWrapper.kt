package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.isString
import skript.notSupported
import skript.values.*

class SkCollectionWrapper<T>(val collection: Collection<T>, val elementCodec: SkCodec<T>) : SkObject() {
    override val klass: SkClassDef
        get() = SkCollectionWrapperClassDef

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        if (key.isString("size"))
            return SkNumber.valueOf(collection.size)

        return super.findMember(key, state)
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("size"))
            notSupported("Can't set size of collections")

        super.setMember(key, value, state)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        if (key.isString("size"))
            return true

        return super.hasOwnMember(key, state)
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        if (key.isString("size"))
            notSupported("Can't delete size of collections")

        super.deleteMember(key, state)
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(collection.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert collections into numbers")
    }
}

object SkCollectionWrapperClassDef : SkClassDef("CollectionWrapper", null)

class SkCollectionIterator<T>(val iterator: Iterator<T>, val elementCodec: SkCodec<T>) {
    // TODO
}

object SkCollectionIteratorClassDef : SkClassDef("CollectionIterator", null)

class SkCodecNativeCollection<T>(val elementCodec: SkCodec<T>) : SkCodec<Collection<T>> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return kotlinVal is Collection<*> // TODO: check sub type somehow?
    }

    override suspend fun toKotlin(value: SkValue, env: SkriptEnv): Collection<T> {
        if (value is SkCollectionWrapper<*>) {
            // TODO: do innards? for now we'll just assume it's right
            return value.collection as Collection<T>
        } else {
            TODO("Can't convert to native collections yet")
        }
    }

    override suspend fun toSkript(value: Collection<T>, env: SkriptEnv): SkValue {
        return SkCollectionWrapper(value, elementCodec)
    }
}

