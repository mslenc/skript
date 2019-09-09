package skript.interop

import skript.exec.RuntimeState
import skript.interop.wrappers.SkByteArray
import skript.io.toSkript
import skript.typeError
import skript.values.SkMap
import skript.values.SkValue

interface SkCodec<T> {
    fun isMatch(kotlinVal: Any): Boolean

    suspend fun toSkript(value: T, state: RuntimeState): SkValue
    suspend fun toKotlin(value: SkValue, state: RuntimeState): T
}

class SkCodecNativeObject<T: Any>(val klass: SkNativeClassDef<T>): SkCodec<T> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return klass.nativeClass.isInstance(kotlinVal)
    }

    override suspend fun toSkript(value: T, state: RuntimeState): SkValue {
        return SkNativeObject(value, klass)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun toKotlin(value: SkValue, state: RuntimeState): T {
        if (value is SkNativeObject<*>) {
            return when {
                value.klass == klass -> value.nativeObj as T
                klass.nativeClass.isInstance(value.nativeObj) -> value.nativeObj as T
                else -> typeError("Can't convert native object from ${value.nativeObj::class} to ${klass.nativeClass}")
            }
        }

        if (value is SkMap) {
            klass.constructor?.let { constructor ->
                val mapAsArgs = value.asArgs()
                val constructed = constructor.call(emptyList(), mapAsArgs, state)
                return constructed.nativeObj
            }
            typeError("Can't construct ${klass.nativeClass} from a Map because the constructor is not defined")
        }

        typeError("Don't know how to convert ${value.asString().value} to ${klass.nativeClass}")
    }
}

private fun SkMap.asArgs(): Map<String, SkValue> {
    val result = HashMap<String, SkValue>()
    this.props.forEach { key, value ->
        result[key] = value
    }
    return result
}

object SkCodecDouble : SkCodec<Double> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Double
    override suspend fun toSkript(value: Double, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asNumber().toDouble()
}

object SkCodecFloat : SkCodec<Float> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Float
    override suspend fun toSkript(value: Float, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asNumber().toDouble().toFloat()
}

object SkCodecLong : SkCodec<Long> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Long
    override suspend fun toSkript(value: Long, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asNumber().toLong()
}

object SkCodecInt : SkCodec<Int> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Int
    override suspend fun toSkript(value: Int, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asNumber().toInt()
}

object SkCodecShort : SkCodec<Short> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Short
    override suspend fun toSkript(value: Short, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asNumber().toInt().toShort()
}

object SkCodecByte : SkCodec<Byte> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Byte
    override suspend fun toSkript(value: Byte, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asNumber().toInt().toByte()
}

object SkCodecChar : SkCodec<Char> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Char
    override suspend fun toSkript(value: Char, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState): Char {
        val str = value.asString().value

        return when {
            str.isEmpty() -> typeError("Can't convert an empty string to char")
            str.length > 1 -> typeError("Can't convert a multi-char string to char")
            else -> str[0]
        }
    }
}

object SkCodecBoolean : SkCodec<Boolean> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Boolean
    override suspend fun toSkript(value: Boolean, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asBoolean().value
}

object SkCodecString : SkCodec<String> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is String
    override suspend fun toSkript(value: String, state: RuntimeState) = value.toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asString().value
}

object SkCodecCharSequence : SkCodec<CharSequence> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is CharSequence
    override suspend fun toSkript(value: CharSequence, state: RuntimeState) = value.toString().toSkript()
    override suspend fun toKotlin(value: SkValue, state: RuntimeState) = value.asString().value
}

