package skript.interop

import skript.interop.ConversionType.*
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.typeError
import skript.util.SkArguments
import skript.values.*
import java.math.BigDecimal

enum class ConversionType {
    NOT_POSSIBLE,
    EXACT,
    COERCE
}

interface SkCodec<T> {
    /**
     * Converts a native value into a SkValue
     */
    fun toSkript(value: T, env: SkriptEnv): SkValue

    /**
     * Converts a SkValue into a native value
     */
    fun toKotlin(value: SkValue, env: SkriptEnv): T

    /**
     * Returns true if conversion to native value is possible. The intended use is overload resolution when multiple
     * native functions have the same name.
     */
    fun canConvert(value: SkValue): ConversionType
}

class SkCodecNativeObject<T: Any>(val klass: SkNativeClassDef<T>): SkCodec<T> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is HoldsNative<*> -> if (klass.nativeClass.isInstance(value.nativeObj)) EXACT else NOT_POSSIBLE
            is SkMap -> if (klass.constructor != null) COERCE else NOT_POSSIBLE
            else -> NOT_POSSIBLE
        }
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
                return constructor.fastCall(value.asArgs(), env).nativeObj
            }
            typeError("Can't construct ${klass.nativeClass} from a Map because the constructor is not defined")
        }

        typeError("Don't know how to convert ${value.asString().value} to ${klass.nativeClass}")
    }
}

private fun SkMap.asArgs(): SkArguments {
    val result = SkArguments()
    result.spreadKwArgs(entries)
    return result
}

abstract class SkCodecNumeric<T> : SkCodec<T> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkNumber -> EXACT
            is SkNumberObject -> EXACT
            is SkString -> if (value.asNumberOrNull() != null) COERCE else NOT_POSSIBLE
            else -> NOT_POSSIBLE
        }
    }
}

object SkCodecDouble : SkCodecNumeric<Double>() {
    override fun toSkript(value: Double, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toDouble()
}

object SkCodecFloat : SkCodecNumeric<Float>() {
    override fun toSkript(value: Float, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toDouble().toFloat()
}

object SkCodecLong : SkCodecNumeric<Long>() {
    override fun toSkript(value: Long, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toLong()
}

object SkCodecInt : SkCodecNumeric<Int>() {
    override fun toSkript(value: Int, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toInt()
}

object SkCodecShort : SkCodecNumeric<Short>() {
    override fun toSkript(value: Short, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toInt().toShort()
}

object SkCodecByte : SkCodecNumeric<Byte>() {
    override fun toSkript(value: Byte, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asNumber().toInt().toByte()
}

object SkCodecChar : SkCodec<Char> {
    override fun canConvert(value: SkValue): ConversionType {
        return when(value) {
            is SkString,
            is SkStringObject -> if (value.asString().value.length == 1) EXACT else NOT_POSSIBLE
            else -> if (value.asString().value.length == 1) COERCE else NOT_POSSIBLE
        }
    }

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
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkBoolean,
            is SkBooleanObject -> EXACT
            else -> COERCE
        }
    }

    override fun toSkript(value: Boolean, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asBoolean().value
}

object SkCodecString : SkCodec<String> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkString,
            is SkStringObject -> EXACT
            else -> COERCE
        }
    }

    override fun toSkript(value: String, env: SkriptEnv) = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = toKotlin(value)
    fun toKotlin(value: SkValue) = value.asString().value
}

object SkCodecCharSequence : SkCodec<CharSequence> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkString,
            is SkStringObject -> EXACT
            else -> COERCE
        }
    }

    override fun toSkript(value: CharSequence, env: SkriptEnv) = value.toString().toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asString().value
}

object SkCodecBigDecimal : SkCodecNumeric<BigDecimal>() {
    override fun toSkript(value: BigDecimal, env: SkriptEnv): SkValue = value.toSkript()
    override fun toKotlin(value: SkValue, env: SkriptEnv): BigDecimal = value.asNumber().toBigDecimal()
}

object SkCodecUnit : SkCodec<Unit> {
    override fun canConvert(value: SkValue): ConversionType {
        return if (value is SkUndefined) EXACT else COERCE
    }

    override fun toSkript(value: Unit, env: SkriptEnv) = SkUndefined
    override fun toKotlin(value: SkValue, env: SkriptEnv) = Unit
}


object SkCodecSkValue : SkCodec<SkValue> {
    override fun canConvert(value: SkValue): ConversionType {
        return EXACT
    }

    override fun toSkript(value: SkValue, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value
}

object SkCodecSkBoolean : SkCodec<SkBoolean> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkBoolean,
            is SkBooleanObject -> EXACT
            else -> COERCE
        }
    }

    override fun toSkript(value: SkBoolean, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asBoolean()
}

object SkCodecSkList : SkCodec<SkList> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkList -> EXACT
            is SkAbstractList -> COERCE
            else -> NOT_POSSIBLE
        }
    }

    override fun toSkript(value: SkList, env: SkriptEnv) = value

    override fun toKotlin(value: SkValue, env: SkriptEnv): SkList {
        return when (value) {
            is SkList -> value
            is SkAbstractList -> SkList(value)
            else -> typeError("Can't convert object ($value) to SkList")
        }
    }
}

object SkCodecSkMap : SkCodec<SkMap> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkMap -> EXACT
            // TODO: is SkAbstractMap -> COERCE
            else -> NOT_POSSIBLE
        }
    }

    override fun toSkript(value: SkMap, env: SkriptEnv) = value

    override fun toKotlin(value: SkValue, env: SkriptEnv): SkMap {
        return when (value) {
            is SkMap -> value
            // TODO: is SkAbstractMap -> SkMap(value)
            else -> typeError("Can't convert object ($value) to SkMap")
        }
    }
}

object SkCodecSkNumber : SkCodec<SkNumber> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkNumber,
            is SkNumberObject -> EXACT
            is SkString -> if (value.asNumberOrNull() != null) COERCE else NOT_POSSIBLE
            is SkBoolean -> COERCE
            else -> NOT_POSSIBLE
        }
    }

    override fun toSkript(value: SkNumber, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asNumber()
}

object SkCodecSkDouble : SkCodec<SkDouble> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkDouble -> EXACT
            is SkNumberObject -> if (value.value is SkDouble) EXACT else COERCE
            is SkNumber -> COERCE
            is SkString -> if (value.asNumberOrNull() != null) COERCE else NOT_POSSIBLE
            is SkBoolean -> COERCE
            else -> NOT_POSSIBLE
        }
    }

    override fun toSkript(value: SkDouble, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asNumber().asDouble()
}

object SkCodecSkDecimal : SkCodec<SkDecimal> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkDecimal -> EXACT
            is SkNumberObject -> if (value.value is SkDecimal) EXACT else COERCE
            is SkNumber -> COERCE
            is SkString -> if (value.asNumberOrNull() != null) COERCE else NOT_POSSIBLE
            is SkBoolean -> COERCE
            else -> NOT_POSSIBLE
        }
    }

    override fun toSkript(value: SkDecimal, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asNumber().asDecimal()
}

object SkCodecSkString : SkCodec<SkString> {
    override fun canConvert(value: SkValue): ConversionType {
        return when (value) {
            is SkString,
            is SkStringObject -> EXACT
            else -> COERCE
        }
    }

    override fun toSkript(value: SkString, env: SkriptEnv) = value
    override fun toKotlin(value: SkValue, env: SkriptEnv) = value.asString()
}