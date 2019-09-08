package skript.interop

import skript.io.toSkript
import skript.typeError
import skript.values.SkValue

interface SkCodec<T> {
    fun isMatch(kotlinVal: Any): Boolean

    fun toSkript(value: T): SkValue
    fun toKotlin(value: SkValue): T
}

object SkCodecDouble : SkCodec<Double> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Double
    override fun toSkript(value: Double) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asNumber().toDouble()
}

object SkCodecFloat : SkCodec<Float> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Float
    override fun toSkript(value: Float) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asNumber().toDouble().toFloat()
}

object SkCodecLong : SkCodec<Long> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Long
    override fun toSkript(value: Long) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asNumber().toLong()
}

object SkCodecInt : SkCodec<Int> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Int
    override fun toSkript(value: Int) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asNumber().toInt()
}

object SkCodecShort : SkCodec<Short> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Short
    override fun toSkript(value: Short) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asNumber().toInt().toShort()
}

object SkCodecByte : SkCodec<Byte> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Byte
    override fun toSkript(value: Byte) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asNumber().toInt().toByte()
}

object SkCodecChar : SkCodec<Char> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is Char
    override fun toSkript(value: Char) = value.toSkript()
    override fun toKotlin(value: SkValue): Char {
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
    override fun toSkript(value: Boolean) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asBoolean().value
}

object SkCodecString : SkCodec<String> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is String
    override fun toSkript(value: String) = value.toSkript()
    override fun toKotlin(value: SkValue) = value.asString().value
}

object SkCodecCharSequence : SkCodec<CharSequence> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is CharSequence
    override fun toSkript(value: CharSequence) = value.toString().toSkript()
    override fun toKotlin(value: SkValue) = value.asString().value
}