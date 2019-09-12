package skript.values

import skript.exec.RuntimeState
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
    abstract suspend fun call(args: SkArguments, state: RuntimeState): SkValue
    abstract suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue

    abstract suspend fun contains(key: SkValue, state: RuntimeState): Boolean

    abstract suspend fun propSet(key: String, value: SkValue, state: RuntimeState)
    abstract suspend fun propGet(key: String, state: RuntimeState): SkValue

    abstract suspend fun elementSet(key: SkValue, value: SkValue, state: RuntimeState)
    abstract suspend fun elementGet(key: SkValue, state: RuntimeState): SkValue
    abstract suspend fun elementDelete(key: SkValue, state: RuntimeState): Boolean

    abstract fun getKind(): SkValueKind

    abstract fun asBoolean(): SkBoolean
    abstract fun asNumber(): SkNumber
    abstract fun asString(): SkString
    abstract fun asObject(): SkObject

    open suspend fun makeIterator(): SkIterator? {
        return null
    }

    abstract suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue
}