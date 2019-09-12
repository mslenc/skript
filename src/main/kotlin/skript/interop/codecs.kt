package skript.interop

import skript.io.SkriptEnv
import skript.io.toSkript
import skript.typeError
import skript.util.SkArguments
import skript.values.*
import java.math.BigDecimal

interface SkCodec<T> {
    fun isMatch(kotlinVal: Any): Boolean

    fun toSkript(value: T, env: SkriptEnv): SkValue
    fun toKotlin(value: SkValue, env: SkriptEnv): T
}

class SkCodecNativeObject<T: Any>(val klass: SkNativeClassDef<T>): SkCodec<T> {
    override fun isMatch(kotlinVal: Any): Boolean {
        return klass.nativeClass.isInstance(kotlinVal)
    }

    override fun toSkript(value: T, env: SkriptEnv): SkValue {
        return SkNativeObject(value, klass)
    }

    @Suppress("UNCHECKED_CAST")
    override fun toKotlin(value: SkValue, env: SkriptEnv): T {
        if (value is SkNativeObject<*>) {
            return when {
                value.klass == klass -> value.nativeObj as T
                klass.nativeClass.isInstance(value.nativeObj) -> value.nativeObj as T
                else -> typeError("Can't convert native object from ${value.nativeObj::class} to ${klass.nativeClass}")
            }
        }

        if (value is SkMap) {
            klass.constructor?.let { constructor ->
                return constructor.call(value.asArgs(), env).nativeObj
            }
            typeError("Can't construct ${klass.nativeClass} from a Map because the constructor is not defined")
        }

        typeError("Don't know how to convert ${value.asString().value} to ${klass.nativeClass}")
    }
}

private fun SkMap.asArgs(): SkArguments {
    val result = SkArguments()
    result.spreadKwArgs(elements)
    return result
}

object SkCodecDouble : SkCodec<Double> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Double
    override fun toSkript(value: Double, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toDouble()
}

object SkCodecFloat : SkCodec<Float> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Float
    override fun toSkript(value: Float, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toDouble().toFloat()
}

object SkCodecLong : SkCodec<Long> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Long
    override fun toSkript(value: Long, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toLong()
}

object SkCodecInt : SkCodec<Int> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Int
    override fun toSkript(value: Int, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toInt()
}

object SkCodecShort : SkCodec<Short> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Short
    override fun toSkript(value: Short, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toInt().toShort()
}

object SkCodecByte : SkCodec<Byte> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Byte
    override fun toSkript(value: Byte, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toInt().toByte()
}

object SkCodecChar : SkCodec<Char> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Char
    override fun toSkript(value: Char, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue): Char {
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
    override fun toSkript(value: Boolean, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asBoolean().value
}

object SkCodecString : SkCodec<String> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is String
    override fun toSkript(value: String, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asString().value
}

object SkCodecCharSequence : SkCodec<CharSequence> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is CharSequence
    override fun toSkript(value: CharSequence, env: SkriptEnv) = value.toString().toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asString().value
}

object SkCodecBigDecimal : SkCodec<BigDecimal> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is BigDecimal
    override fun toSkript(value: BigDecimal, env: SkriptEnv): SkValue = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv): BigDecimal = value.asNumber().toBigDecimal()
}

object SkCodecUnit : SkCodec<Unit> {
    override fun isMatch(kotlinVal: Any) = kotlinVal == Unit
    override fun toSkript(value: Unit, env: SkriptEnv) = SkUndefined
    override fun toKotlin(value: SkValue, env: SkriptEnv) = Unit
}


object SkCodecSkValue : SkCodec<SkValue> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkValue
    override fun toSkript(value: SkValue, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value
}

object SkCodecSkBoolean : SkCodec<SkBoolean> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkBoolean
    override fun toSkript(value: SkBoolean, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asBoolean()
}

object SkCodecSkList : SkCodec<SkList> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkList
    override fun toSkript(value: SkList, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value as SkList
}

object SkCodecSkMap : SkCodec<SkMap> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkMap
    override fun toSkript(value: SkMap, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value as SkMap
}

object SkCodecSkNumber : SkCodec<SkNumber> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkNumber
    override fun toSkript(value: SkNumber, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asNumber()
}

object SkCodecSkDouble : SkCodec<SkDouble> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkDouble
    override fun toSkript(value: SkDouble, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asNumber().asDouble()
}

object SkCodecSkDecimal : SkCodec<SkDecimal> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkDecimal
    override fun toSkript(value: SkDecimal, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asNumber().asDecimal()
}

object SkCodecSkString : SkCodec<SkString> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is SkString
    override fun toSkript(value: SkString, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asString()
}