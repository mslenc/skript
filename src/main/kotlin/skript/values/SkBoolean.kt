package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments
import java.lang.StringBuilder

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

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        typeError("$exprDebug evaluated to a boolean, so range can't be created")
    }

    override fun toString(sb: StringBuilder) {
        sb.append(if (value) "true" else "false")
    }

    companion object {
        val TRUE = SkBoolean(true)
        val FALSE = SkBoolean(false)

        fun valueOf(value: Boolean) = if (value) TRUE else FALSE
    }
}

class SkBooleanObject(override val value: SkBoolean) : SkScalarObject() {
    override val klass: SkClassDef
        get() = SkBooleanClassDef

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

object SkBooleanClassDef : SkClassDef("Boolean", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val valArg = args.getParam("value")
        return SkBooleanObject(valArg.asBoolean())
    }
}