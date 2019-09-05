package skript.values

import skript.exec.RuntimeState
import skript.typeError

class SkBoolean private constructor(val value: Boolean) : SkScalar() {
    override fun getKind(): SkValueKind {
        return SkValueKind.BOOLEAN
    }

    override fun asBoolean(): SkBoolean {
        return this
    }

    override fun asString(): SkString {
        return if (value) SkString.TRUE else SkString.FALSE
    }

    override fun asNumber(): SkNumber {
        return if (value) SkNumber.ONE else SkNumber.ZERO
    }

    override fun asObject(): SkObject {
        return if (value) SkBooleanObject.TRUE else SkBooleanObject.FALSE
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        typeError("Can't make ranges from booleans")
    }

    companion object {
        val TRUE = SkBoolean(true)
        val FALSE = SkBoolean(false)

        fun valueOf(value: Boolean) = if (value) TRUE else FALSE
    }
}

class SkBooleanObject(override val value: SkBoolean) : SkScalarObject(BooleanClass) {
    override fun asString(): SkString {
        return value.asString()
    }

    override fun asNumber(): SkNumber {
        return value.asNumber()
    }

    override fun asBoolean(): SkBoolean {
        return value
    }

    companion object {
        val TRUE = SkBooleanObject(SkBoolean.TRUE)
        val FALSE = SkBooleanObject(SkBoolean.FALSE)
    }
}