package skript.values

import skript.io.SkriptEnv
import skript.opcodes.SkIterator
import skript.util.SkArguments

enum class SkValueKind {
    NULL,
    UNDEFINED,

    NUMBER,
    DECIMAL,
    BOOLEAN,
    STRING,

    LIST,
    MAP,

    CLASS,
    OBJECT,
    FUNCTION,
    METHOD
}

abstract class SkValue {
    abstract suspend fun call(args: SkArguments, env: SkriptEnv): SkValue
    abstract suspend fun callMethod(methodName: String, args: SkArguments, env: SkriptEnv, exprDebug: String): SkValue

    abstract suspend fun contains(key: SkValue, env: SkriptEnv): Boolean

    abstract suspend fun propertySet(key: String, value: SkValue, env: SkriptEnv)
    abstract suspend fun propertyGet(key: String, env: SkriptEnv): SkValue

    abstract suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv)
    abstract suspend fun entryGet(key: SkValue, env: SkriptEnv): SkValue
    abstract suspend fun entryDelete(key: SkValue, env: SkriptEnv): Boolean

    abstract fun getKind(): SkValueKind

    abstract fun asBoolean(): SkBoolean
    abstract fun asNumber(): SkNumber
    abstract fun asString(): SkString
    abstract fun asObject(): SkObject

    open suspend fun makeIterator(): SkIterator? {
        return null
    }

    abstract suspend fun makeRange(end: SkValue, endInclusive: Boolean, env: SkriptEnv, exprDebug: String): SkValue

    abstract fun unwrap(): Any?
}