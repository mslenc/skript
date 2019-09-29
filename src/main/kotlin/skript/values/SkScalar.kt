package skript.values

import skript.io.SkriptEnv
import skript.opcodes.SkIterator
import skript.typeError
import skript.util.SkArguments

abstract class SkScalar : SkValue() {
    final override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val sb = StringBuilder("Can't call ")
        toString(sb)
        typeError(sb.toString())
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, env: SkriptEnv, exprDebug: String): SkValue {
        return asObject().callMethod(methodName, args, env, exprDebug)
    }

    override suspend fun contains(key: SkValue, env: SkriptEnv): Boolean {
        return false
    }

    override suspend fun propertySet(key: String, value: SkValue, env: SkriptEnv) {
        typeError("Can't set properties on scalars")
    }

    override suspend fun propertyGet(key: String, env: SkriptEnv): SkValue {
        return SkUndefined
    }

    override suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv) {
        typeError("Can't set elements on scalars")
    }

    override suspend fun entryGet(key: SkValue, env: SkriptEnv): SkValue {
        return SkUndefined
    }

    override suspend fun entryDelete(key: SkValue, env: SkriptEnv): Boolean {
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

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, env: SkriptEnv, exprDebug: String): SkValue {
        return value.makeRange(end, endInclusive, env, exprDebug)
    }
}