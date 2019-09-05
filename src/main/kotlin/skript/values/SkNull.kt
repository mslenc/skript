package skript.values

import skript.exec.RuntimeState
import skript.typeError

object SkNull : SkValue() {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        typeError("Can't call null")
    }

    override suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        typeError("Can't call methods on null")
    }

    override suspend fun findMember(key: SkValue): SkValue {
        return SkUndefined
    }

    override suspend fun findMember(key: String): SkValue {
        return SkUndefined
    }

    override suspend fun hasOwnMember(key: SkValue): Boolean {
        return false
    }

    override suspend fun deleteMember(key: SkValue) {
        // ignore
    }

    override suspend fun deleteMember(key: String) {
        // ignore
    }

    override fun asObject(): SkObject {
        typeError("Can't convert null into an object")
    }

    override suspend fun setMember(key: SkValue, value: SkValue) {
        typeError("Can't set members on null")
    }

    override suspend fun setMember(key: String, value: SkValue) {
        typeError("Can't set members on null")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        typeError("Can't make a range with null")
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
}