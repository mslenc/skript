package skript.values

import skript.exec.RuntimeState
import skript.typeError

object SkUndefined : SkScalar() {
    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        return false
    }

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        return SkUndefined
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        // ignore
    }

    override fun asObject(): SkObject {
        typeError("Can't convert undefined into an object")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        typeError("Can't set members on undefined")
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.UNDEFINED
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.FALSE
    }

    override fun asNumber(): SkNumber {
        typeError("Can't convert undefined into a number")
    }

    override fun asString(): SkString {
        return SkString.UNDEFINED
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        typeError("$exprDebug evaluated to undefined, so range can't be created")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("undefined")
    }

    override suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState, exprDebug: String): SkValue {
        typeError("$exprDebug is undefined, so can't call methods on it")
    }
}