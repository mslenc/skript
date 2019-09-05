package skript.values

import skript.exec.RuntimeState
import skript.notSupported

abstract class SkScalar : SkValue() {
    final override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        notSupported("Can't call scalars")
    }

    final override suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        return asObject().callMethod(methodName, posArgs, kwArgs, state)
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

    override suspend fun setMember(key: SkValue, value: SkValue) {
        notSupported("Can't set members on scalars")
    }

    override suspend fun setMember(key: String, value: SkValue) {
        notSupported("Can't set members on scalars")
    }

    override suspend fun deleteMember(key: SkValue) {
        // nothing
    }

    override suspend fun deleteMember(key: String) {
        // nothing
    }
}

abstract class SkScalarObject(klass: SkClass) : SkObject(klass) {
    abstract val value: SkScalar

    override suspend fun makeIterator(): SkValue {
        return value.makeIterator()
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        return value.makeRange(end, endInclusive, state)
    }
}