package skript.interop.wrappers

import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.typeError
import skript.values.*

class SkCollectionWrapper<T>(val collection: Collection<T>, val elementCodec: SkCodec<T>, val env: SkriptEnv) : SkAbstractList() {
    override val klass: SkClassDef
        get() = SkCollectionWrapperClassDef

    override fun getSize(): Int {
        return collection.size
    }

    override fun getValidSlot(index: Int): SkValue {
        typeError("This collection doesn't support random access, use iteration instead")
    }

    override suspend fun makeIterator(): SkIterator {
        return SkCollectionIterator(collection.iterator(), elementCodec, env)
    }
}

object SkCollectionWrapperClassDef : SkClassDef("CollectionWrapper", SkAbstractListClassDef)

class SkCollectionIterator<T>(val iterator: Iterator<T>, val elementCodec: SkCodec<T>, val env: SkriptEnv): SkIterator() {
    override val klass: SkClassDef
        get() = SkCollectionIteratorClassDef

    private var index = -1
    private var value: SkValue = SkUndefined

    override fun moveToNext(): Boolean {
        if (iterator.hasNext()) {
            index++
            value = elementCodec.toSkript(iterator.next(), env)
            return true
        }

        return false
    }

    override fun getCurrentKey(): SkValue {
        return index.toSkript()
    }

    override fun getCurrentValue(): SkValue {
        return value
    }
}

object SkCollectionIteratorClassDef : SkClassDef("CollectionIterator", SkIteratorClassDef)

class SkCodecNativeCollection<T>(val elementCodec: SkCodec<T>) : SkCodec<Collection<T>> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return kotlinVal is Collection<*> // TODO: check sub type somehow?
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): Collection<T> {
        if (value is SkCollectionWrapper<*>) {
            // TODO: do innards? for now we'll just assume it's right
            return value.collection as Collection<T>
        } else {
            TODO("Can't convert to native collections yet")
        }
    }

    override fun toSkript(value: Collection<T>, env: SkriptEnv): SkValue {
        return SkCollectionWrapper(value, elementCodec, env)
    }
}

