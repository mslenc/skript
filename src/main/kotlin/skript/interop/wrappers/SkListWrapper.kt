package skript.interop.wrappers

import skript.interop.ConversionType
import skript.interop.HoldsNative
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.typeError
import skript.values.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class SkListWrapper<T>(override val nativeObj: List<T>, val elementCodec: SkCodec<T>, val env: SkriptEnv) : SkAbstractList(), HoldsNative<List<T>> {
    override val klass: SkClassDef
        get() = SkListWrapperClassDef

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return elementCodec.toSkript(nativeObj[index], env)
    }
}

object SkListWrapperClassDef : SkClassDef("ListWrapper", SkAbstractListClassDef)

class SkCodecNativeList<T>(val listClass: KClass<*>, val elementCodec: SkCodec<T>) : SkCodec<List<T>> {
    override fun canConvert(value: SkValue): ConversionType {
        if (value is SkListWrapper<*> && value.elementCodec == elementCodec)
            if (listClass.isInstance(value.nativeObj))
                return ConversionType.EXACT

        if (value is SkAbstractList) {
            return when {
                listClass.isSuperclassOf(ArrayList::class) -> {
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

    override fun toKotlin(value: SkValue, env: SkriptEnv): List<T> {
        if (value is SkListWrapper<*> && value.elementCodec == elementCodec)
            if (listClass.isInstance(value.nativeObj))
                return value.nativeObj as List<T>

        if (value is SkAbstractList) {
            val container: MutableList<T> = when {
                listClass.isSuperclassOf(ArrayList::class) -> ArrayList()
                else -> typeError("Can't convert $value to $listClass")
            }

            for (i in 0 until value.getSize()) {
                container.add(elementCodec.toKotlin(value.getSlot(i), env))
            }

            return container
        }

        typeError("Can't convert $value to $listClass")
    }

    override fun toSkript(value: List<T>, env: SkriptEnv): SkValue {
        return SkListWrapper(value, elementCodec, env)
    }
}