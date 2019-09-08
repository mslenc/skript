package skript.values

import skript.exec.RuntimeState

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
    abstract suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue
    abstract suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState, exprDebug: String): SkValue

    abstract suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean
    abstract suspend fun findMember(key: SkValue, state: RuntimeState): SkValue
    abstract suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState)
    abstract suspend fun deleteMember(key: SkValue, state: RuntimeState)

    abstract fun getKind(): SkValueKind

    abstract fun asBoolean(): SkBoolean
    abstract fun asNumber(): SkNumber
    abstract fun asString(): SkString
    abstract fun asObject(): SkObject

    open suspend fun makeIterator(): SkValue {
        return SkUndefined
    }

    abstract suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue
}