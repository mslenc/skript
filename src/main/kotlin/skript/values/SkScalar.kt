package skript.values

import skript.exec.RuntimeState
import skript.opcodes.SkIterator
import skript.typeError
import skript.util.SkArguments

abstract class SkScalar : SkValue() {
    final override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        val sb = StringBuilder("Can't call ")
        toString(sb)
        typeError(sb.toString())
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue {
        return asObject().callMethod(methodName, args, state, exprDebug)
    }

    override suspend fun contains(key: SkValue, state: RuntimeState): Boolean {
        return false
    }

    override suspend fun propertySet(key: String, value: SkValue, state: RuntimeState) {
        typeError("Can't set properties on scalars")
    }

    override suspend fun propertyGet(key: String, state: RuntimeState): SkValue {
        return SkUndefined
    }

    override suspend fun entrySet(key: SkValue, value: SkValue, state: RuntimeState) {
        typeError("Can't set elements on scalars")
    }

    override suspend fun entryGet(key: SkValue, state: RuntimeState): SkValue {
        return SkUndefined
    }

    override suspend fun entryDelete(key: SkValue, state: RuntimeState): Boolean {
        return false
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

    override suspend fun makeIterator(): SkIterator? {
        return value.makeIterator()
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        return value.makeRange(end, endInclusive, state, exprDebug)
    }
}