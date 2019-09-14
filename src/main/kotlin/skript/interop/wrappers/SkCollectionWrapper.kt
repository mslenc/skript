package skript.interop.wrappers

import skript.interop.ConversionType
import skript.interop.HoldsNative
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.typeError
import skript.values.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class SkCollectionWrapper<T>(override val nativeObj: Collection<T>, val elementCodec: SkCodec<T>, val env: SkriptEnv) : SkAbstractList(), HoldsNative<Collection<T>> {
    override val klass: SkClassDef
        get() = SkCollectionWrapperClassDef

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        typeError("This collection doesn't support random access, use iteration instead")
    }

    override suspend fun makeIterator(): SkIterator {
        return SkCollectionIterator(nativeObj.iterator(), elementCodec, env)
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

class SkCodecNativeCollection<T>(val collectionClass: KClass<*>, val elementCodec: SkCodec<T>) : SkCodec<Collection<T>> {
    override fun canConvert(value: SkValue): ConversionType {
        if (value is SkCollectionWrapper<*> && value.elementCodec == elementCodec)
            if (collectionClass.isInstance(value.nativeObj))
                return ConversionType.EXACT

        if (value is SkAbstractList) {
            return when {
                collectionClass.isSuperclassOf(ArrayList::class) ||
                collectionClass.isSuperclassOf(LinkedHashSet::class) -> {
                    for (i in 0 until value.getSize()) {
                        if (elementCodec.canConvert(value.getSlot(i)) == ConversionType.NOT_POSSIBLE)
                            return ConversionType.NOT_POSSIBLE
                    }
                    ConversionType.COERCE
                }
                else ->
                    ConversionType.NOT_POSSIBLE
            }
        }

        return ConversionType.NOT_POSSIBLE
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): Collection<T> {
        if (value is SkCollectionWrapper<*> && value.elementCodec == elementCodec)
            if (collectionClass.isInstance(value.nativeObj))
                return value.nativeObj as Collection<T>

        if (value is SkAbstractList) {
            val container: MutableCollection<T> = when {
                collectionClass.isSuperclassOf(ArrayList::class) -> ArrayList()
                collectionClass.isSuperclassOf(LinkedHashSet::class) -> LinkedHashSet()
                else -> typeError("Can't convert $value to $collectionClass")
            }

            for (i in 0 until value.getSize()) {
                container.add(elementCodec.toKotlin(value.getSlot(i), env))
            }

            return container
        }

        typeError("Can't convert $value to $collectionClass")
    }

    override fun toSkript(value: Collection<T>, env: SkriptEnv): SkValue {
        return SkCollectionWrapper(value, elementCodec, env)
    }
}

