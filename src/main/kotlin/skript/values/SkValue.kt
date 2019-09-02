package skript.values

import skript.exec.RuntimeState

enum class SkValueKind {
    NULL,
    UNDEFINED,

    NUMBER,
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
    abstract suspend fun callMethod(methodName: String, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue

    abstract suspend fun hasOwnMember(key: SkValue): Boolean

    abstract suspend fun findMember(key: SkValue): SkValue
    abstract suspend fun findMember(key: String): SkValue

    abstract suspend fun setMember(key: SkValue, value: SkValue)
    abstract suspend fun setMember(key: String, value: SkValue)

    abstract suspend fun deleteMember(key: SkValue)
    abstract suspend fun deleteMember(key: String)

    abstract fun getKind(): SkValueKind

    abstract fun asBoolean(): SkBoolean
    abstract fun asNumber(): SkNumber
    abstract fun asString(): SkString
    abstract fun asObject(): SkObject
}