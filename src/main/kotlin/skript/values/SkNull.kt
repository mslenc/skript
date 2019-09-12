package skript.values

import skript.exec.RuntimeState
import skript.typeError
import skript.util.SkArguments

object SkNull : SkScalar() {
    override fun asObject(): SkObject {
        typeError("Can't convert null into an object")
    }

    override suspend fun propSet(key: String, value: SkValue, state: RuntimeState) {
        typeError("Can't set properties on null")
    }

    override suspend fun elementSet(key: SkValue, value: SkValue, state: RuntimeState) {
        typeError("Can't set elements on null")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        typeError("$exprDebug evaluated to null, so range can't be created")
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.NULL
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.FALSE
    }

    override fun asNumber(): SkNumber {
        return SkNumber.ZERO
    }

    override fun asString(): SkString {
        return SkString.NULL
    }

    override fun toString(sb: StringBuilder) {
        sb.append("null")
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue {
        typeError("$exprDebug is null, so can't call methods on it")
    }
}