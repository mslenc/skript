package skript.values

import skript.isInteger
import java.math.BigDecimal
import java.math.BigInteger

class SkNumber private constructor (val value: BigDecimal) : SkScalar() {
    override fun getKind(): SkValueKind {
        return SkValueKind.NUMBER
    }

    override fun asNumber(): SkNumber {
        return this
    }

    override fun asString(): SkString {
        return SkString(value.toPlainString())
    }

    override fun asObject(): SkObject {
        return SkNumberObject(this)
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value.signum() != 0)
    }

    fun isInt(): Boolean {
        return value.isInteger() && value in MIN_INT..MAX_INT
    }

    fun isNonNegativeInt(): Boolean {
        return when {
            isInt() -> value.toInt() >= 0
            else -> false
        }
    }

    fun negate(): SkNumber {
        return SkNumber(value.negate())
    }

    companion object {
        val MINUS_ONE = valueOf(-1)
        val ZERO = valueOf(0)
        val ONE = valueOf(1)

        private val MIN_INT = Int.MIN_VALUE.toBigDecimal()
        private val MAX_INT = Int.MAX_VALUE.toBigDecimal()

        // TODO - cache some others?

        fun valueOf(value: Int): SkNumber = when (value) {
            0 -> ZERO
            1 -> ONE
            -1 -> MINUS_ONE
            else -> SkNumber(value.toBigDecimal())
        }

        fun valueOf(value: Long): SkNumber = SkNumber(value.toBigDecimal())
        fun valueOf(value: Double): SkNumber = SkNumber(value.toBigDecimal())
        fun valueOf(value: BigInteger): SkNumber = SkNumber(value.toBigDecimal())
        fun valueOf(value: BigDecimal): SkNumber = SkNumber(value)
    }
}

class SkNumberObject(override val value: SkNumber): SkScalarObject(NumberClass) {
    override fun asBoolean(): SkBoolean {
        return value.asBoolean()
    }

    override fun asNumber(): SkNumber {
        return value
    }

    override fun asString(): SkString {
        return value.asString()
    }
}


// TODO: use the other implementations, like below

/*


sealed class SkNumber : SkScalar() {
    abstract val value: Number

    override fun getKind(): SkValueKind {
        return SkValueKind.NUMBER
    }

    override fun asNumber(): SkNumber {
        return this
    }

    override fun asString(): SkString {
        return SkString(value.toString())
    }

    override fun asObject(): SkObject {
        return SkNumberObject(this)
    }

    abstract fun isInt(): Boolean

    fun isNonNegativeInt(): Boolean {
        return when {
            isInt() -> value.toInt() >= 0
            else -> false
        }
    }

    abstract fun negate(): SkNumber

    companion object {
        val ZERO = SkInt(0)
        val ONE = SkInt(1)
        val MINUS_ONE = SkInt(-1)

        // TODO - cache some?

        fun valueOf(value: Int): SkNumber = SkInt(value)
        fun valueOf(value: Long): SkNumber = SkLong(value)
        fun valueOf(value: Double): SkNumber = SkDouble(value)
        fun valueOf(value: BigInteger): SkNumber = SkBigInt(value)
        fun valueOf(value: BigDecimal): SkNumber = SkBigDecimal(value)
    }
}

class SkNumberObject(val value: SkNumber): SkObject(NumberClass) {
    override fun asBoolean(): SkBoolean {
        return value.asBoolean()
    }

    override fun asNumber(): SkNumber {
        return value
    }

    override fun asString(): SkString {
        return value.asString()
    }
}

class SkDouble(override val value: Double): SkNumber() {
    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value != 0.0 && !value.isNaN())
    }

    override fun isInt() = value.toInt().toDouble() == value

    override fun negate(): SkNumber {
        return SkDouble(-value)
    }
}

class SkInt(override val value: Int): SkNumber() {
    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value != 0)
    }

    override fun isInt() = true

    override fun negate(): SkNumber {
        return if (value != Int.MIN_VALUE) {
            SkInt(-value)
        } else {
            SkLong(-value.toLong())
        }
    }
}

class SkLong(override val value: Long): SkNumber() {
    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value != 0L)
    }

    override fun negate(): SkNumber {
        return if (value != Long.MIN_VALUE) {
            SkLong(-value)
        } else {
            SkBigInt(value.toBigInteger().negate())
        }
    }

    override fun isInt() = value in Int.MIN_VALUE.toLong() .. Int.MAX_VALUE.toLong()
}

class SkBigInt(override val value: BigInteger): SkNumber() {
    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value.signum() != 0)
    }

    override fun isInt(): Boolean {
        return value in MIN_INT..MAX_INT
    }

    override fun negate(): SkNumber {
        return SkBigInt(value.negate())
    }

    companion object {
        private val MIN_INT = Int.MIN_VALUE.toBigInteger()
        private val MAX_INT = Int.MAX_VALUE.toBigInteger()
    }
}

class SkBigDecimal(override val value: BigDecimal): SkNumber() {
    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value.signum() != 0)
    }

    override fun isInt(): Boolean {
        return value.isInteger() && value in MIN_INT..MAX_INT
    }

    override fun negate(): SkNumber {
        return SkBigDecimal(value.negate())
    }

    companion object {
        private val MIN_INT = Int.MIN_VALUE.toBigDecimal()
        private val MAX_INT = Int.MAX_VALUE.toBigDecimal()
    }
}*/