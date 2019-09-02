package skript.values

import skript.exec.RuntimeState
import skript.notSupported

object SkUndefined : SkValue() {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        notSupported("Can't call undefined")
    }

    override suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        notSupported("Can't call $methodName on undefined")
    }

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
        notSupported("Can't convert undefined into an object")
    }

    override suspend fun setMember(key: SkValue, value: SkValue) {
        notSupported("Can't set members on undefined")
    }

    override suspend fun setMember(key: String, value: SkValue) {
        notSupported("Can't set members on undefined")
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.UNDEFINED
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.FALSE
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert undefined into a number")
    }

    override fun asString(): SkString {
        return SkString.UNDEFINED
    }
}