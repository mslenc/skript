package skript.interop.wrappers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.interop.ConversionType
import skript.interop.HoldsNative
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.typeError
import skript.values.*
import kotlin.reflect.KClass

class SkArrayWrapper<T>(override val nativeObj: Array<T>, override val elementCodec: SkCodec<T>, val env: SkriptEnv) : SkAbstractNativeArray<Array<T>>() {
    override val klass: SkClassDef
        get() = SkArrayWrapperClassDef

    override fun getSize() = nativeObj.size

    override fun getValidSlot(index: Int): SkValue {
        return elementCodec.toSkript(nativeObj[index], env)
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = elementCodec.toKotlin(value, env)
    }

    override fun unwrap(): Array<T> {
        return nativeObj
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        val list = factory.arrayNode()

        for (i in 0 until getSize())
            list.add(getValidSlot(i).toJson(factory))

        return list
    }
}

object SkArrayWrapperClassDef : SkClassDef("ArrayWrapper", SkAbstractListClassDef)

class SkCodecNativeArray<T>(val arrayClass: KClass<*>, val elementClass: Class<*>?, val elementCodec: SkCodec<T>) : SkCodec<Array<T>> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkArrayWrapper<*> -> if (value.elementCodec == this.elementCodec) ConversionType.EXACT else ConversionType.NOT_POSSIBLE
            is HoldsNative<*> -> if (arrayClass.isInstance(value.nativeObj)) ConversionType.EXACT else ConversionType.NOT_POSSIBLE
            is SkAbstractList -> ConversionType.COERCE
            else -> ConversionType.NOT_POSSIBLE
        }
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): Array<T> {
        when (value) {
            is SkArrayWrapper<*> -> {
                if (value.elementCodec == this.elementCodec) {
                    return value.nativeObj as Array<T>
                } else {
                    typeError("Array type mismatch")
                }
            }
            is HoldsNative<*> -> {
                if (arrayClass.isInstance(value.nativeObj)) {
                    return value.nativeObj as Array<T>
                } else {
                    typeError("Array type mismatch")
                }
            }
            is SkAbstractList -> {
                val array = if (elementClass != null) {
                    java.lang.reflect.Array.newInstance(elementClass, value.getSize()) as Array<T>
                } else {
                    arrayOfNulls<Any?>(value.getSize()) as Array<T>
                }

                for (i in 0 until value.getSize())
                    array[i] = elementCodec.toKotlin(value.getSlot(i), env)
                return array
            }
            else -> typeError("Can't convert to $arrayClass")
        }
    }

    override fun toSkript(value: Array<T>, env: SkriptEnv): SkValue {
        return SkArrayWrapper(value, elementCodec, env)
    }
}