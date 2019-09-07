package skript.values

import skript.exec.RuntimeState
import skript.isInteger
import skript.toStrictNumberOrNull
import skript.typeError
import java.math.BigDecimal
import java.math.BigInteger

sealed class SkNumber : SkScalar(), Comparable<SkNumber> {
    abstract val value: Number

    override fun getKind(): SkValueKind {
        return SkValueKind.NUMBER
    }

    override fun asNumber(): SkNumber {
        return this
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

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        val endNum = end.toStrictNumberOrNull() ?: typeError("Can't make range between a ${getKind()} and a ${end.getKind()}")

        return SkNumberRange(this, endNum, endInclusive)
    }

    override fun toString(sb: StringBuilder) {
        sb.append(value.toString())
    }

    abstract fun signum(): Int

    abstract fun toDouble(): Double

    companion object {
        val MINUS_ONE get() = SkDouble.MINUS_ONE
        val ZERO get() = SkDouble.ZERO
        val ONE get() = SkDouble.ONE

        // TODO - cache some others?

        fun valueOf(value: Int): SkNumber = SkDouble.valueOf(value)
        fun valueOf(value: Double): SkNumber = SkDouble.valueOf(value)

        fun valueOf(value: Long): SkNumber = SkDecimal.valueOf(value)
        fun valueOf(value: BigInteger): SkNumber = SkDecimal.valueOf(value)
        fun valueOf(value: BigDecimal): SkNumber = SkDecimal.valueOf(value)
    }
}

class SkDecimal private constructor (override val value: BigDecimal) : SkNumber() {
    override fun getKind(): SkValueKind {
        return SkValueKind.DECIMAL
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

    override fun isInt(): Boolean {
        return value.isInteger() && value >= MIN_INT && value <= MAX_INT
    }

    override fun negate(): SkNumber {
        return SkDecimal(value.negate())
    }

    override fun compareTo(other: SkNumber): Int {
        return when (other) {
            is SkDecimal -> {
                value.compareTo(other.value)
            }
            is SkDouble -> {
                value.toDouble().compareTo(other.dvalue)
            }
        }
    }

    override fun signum(): Int {
        return value.signum()
    }

    override fun toDouble(): Double {
        return value.toDouble()
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        val endNum = end.toStrictNumberOrNull() ?: typeError("Can't make range between a NUMBER and a ${end.getKind()}")

        return SkNumberRange(this, endNum, endInclusive)
    }

    override fun toString(sb: StringBuilder) {
        sb.append(value).append('d')
    }

    companion object {
        val MINUS_ONE = SkDecimal(BigDecimal.valueOf(-1))
        val ZERO = SkDecimal(BigDecimal.valueOf(0))
        val ONE = SkDecimal(BigDecimal.valueOf(1))

        private val MIN_INT = Int.MIN_VALUE.toBigDecimal()
        private val MAX_INT = Int.MAX_VALUE.toBigDecimal()

        // TODO - cache some others?

        fun valueOf(value: Int): SkNumber = when (value) {
            0 -> ZERO
            1 -> ONE
            -1 -> MINUS_ONE
            else -> SkDecimal(value.toBigDecimal())
        }

        fun valueOf(value: Long): SkDecimal = SkDecimal(value.toBigDecimal())
        fun valueOf(value: Double): SkDecimal = SkDecimal(value.toBigDecimal())
        fun valueOf(value: BigInteger): SkDecimal = SkDecimal(value.toBigDecimal())
        fun valueOf(value: BigDecimal): SkDecimal = SkDecimal(value)
    }
}

class SkDouble private constructor (val dvalue: Double) : SkNumber() {
    override val value: Number
        get() = dvalue

    override fun getKind(): SkValueKind {
        return SkValueKind.NUMBER
    }

    override fun asNumber(): SkNumber {
        return this
    }

    override fun asString(): SkString {
        return SkString(dvalue.toString())
    }

    override fun asObject(): SkObject {
        return SkNumberObject(this)
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(!dvalue.isNaN() && dvalue != 0.0)
    }

    override fun isInt(): Boolean {
        return dvalue.toInt().toDouble() == dvalue
    }

    override fun negate(): SkNumber {
        return valueOf(-dvalue)
    }

    override fun signum(): Int {
        if (dvalue < 0.0) return -1
        if (dvalue > 0.0) return 1
        return 0
    }

    override fun toDouble(): Double {
        return dvalue
    }

    override fun compareTo(other: SkNumber): Int {
        return when (other) {
            is SkDouble -> {
                dvalue.compareTo(other.dvalue)
            }
            is SkDecimal -> {
                dvalue.compareTo(other.value.toDouble())
            }
        }
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        val endNum = end.toStrictNumberOrNull() ?: typeError("Can't make range between a NUMBER and a ${end.getKind()}")

        return SkNumberRange(this, endNum, endInclusive)
    }

    override fun toString(sb: StringBuilder) {
        sb.append(dvalue)
    }

    companion object {
        val MINUS_ONE = SkDouble(-1.0)
        val ZERO = SkDouble(0.0)
        val ONE = SkDouble(1.0)

        // TODO - cache some others?

        fun valueOf(value: Int): SkDouble = when (value) {
            0 -> ZERO
            1 -> ONE
            -1 -> MINUS_ONE
            else -> SkDouble(value.toDouble())
        }

        fun valueOf(value: Long): SkDouble = SkDouble(value.toDouble())
        fun valueOf(value: Double): SkDouble = SkDouble(value)
    }
}

class SkNumberObject(override val value: SkNumber): SkScalarObject() {
    override val klass: SkClass
        get() = NumberClass

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
