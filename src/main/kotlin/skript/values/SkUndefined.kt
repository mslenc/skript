package skript.values

import skript.exec.RuntimeState
import skript.typeError

object SkUndefined : SkScalar() {
    override suspend fun hasOwnMember(key: SkValue): Boolean {
        return false
    }

    override suspend fun findMember(key: SkValue): SkValue {
        return SkUndefined
    }

    override suspend fun findMember(key: String): SkValue {
        return SkUndefined
    }

    override suspend fun deleteMember(key: SkValue) {
        // ignore
    }

    override suspend fun deleteMember(key: String) {
        // ignore
    }

    override fun asObject(): SkObject {
        typeError("Can't convert undefined into an object")
    }

    override suspend fun setMember(key: SkValue, value: SkValue) {
        typeError("Can't set members on undefined")
    }

    override suspend fun setMember(key: String, value: SkValue) {
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

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        typeError("Can't make a range with undefined")
    }

    override fun toString(sb: StringBuilder) {
        sb.append("undefined")
    }
}