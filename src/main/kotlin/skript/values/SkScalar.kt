package skript.values

import skript.exec.RuntimeState
import skript.notSupported

abstract class SkScalar : SkValue() {
    final override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val sb = StringBuilder("Can't call ")
        toString(sb)
        notSupported(sb.toString())
    }

    override suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState, exprDebug: String): SkValue {
        return asObject().callMethod(methodName, posArgs, kwArgs, state, exprDebug)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        return false
    }

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        return SkUndefined
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        notSupported("Can't set members on scalars")
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        // nothing
    }

    final override fun toString(): String {
        val sb = StringBuilder()
        toString(sb)
        return sb.toString()
    }

    abstract fun toString(sb: StringBuilder)
}

abstract class SkScalarObject : SkObject() {
    abstract val value: SkScalar

    override suspend fun makeIterator(): SkValue {
        return value.makeIterator()
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        return value.makeRange(end, endInclusive, state, exprDebug)
    }
}