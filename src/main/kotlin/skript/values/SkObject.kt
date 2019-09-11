package skript.values

import skript.exec.RuntimeState
import skript.illegalArg
import skript.io.toSkript
import skript.notSupported
import skript.util.SkArguments

abstract class SkObject : SkValue() {
    internal var props: Props = EmptyProps
    abstract val klass: SkClassDef

    override fun getKind(): SkValueKind {
        return SkValueKind.OBJECT
    }

    override fun asObject(): SkObject {
        return this
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    override fun asNumber(): SkNumber {
        throw UnsupportedOperationException("Objects can't be converted into numbers")
    }

    override fun asString(): SkString {
        return SkString("[object ${klass.name}]")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        defaultSetMember(key.asString().value, value)
    }

    protected fun defaultSetMember(key: String, value: SkValue) {
        if (value == SkUndefined)
            return defaultDeleteMember(key)

        klass.findInstanceMethod(key)?.let { method ->
            when (value) {
                is SkFunction -> { /* ok - this will be ignored, so it can be any type */ }
                is SkMethod -> {
                    if (!value.expectedClass.isSameOrSuperClassOf(method.expectedClass)) {
                        illegalArg("The method is not compatible - its expectedClass must be same or a super class of the original")
                    }
                }
                else -> {
                    illegalArg("Field $key can only be set to a function or a compatible method")
                }
            }
        }

        props = props.withAdded(key, value)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        return defaultHasOwnMember(key)
    }

    protected fun defaultHasOwnMember(key: SkValue): Boolean {
        val keyStr = key.asString().value

        return props.get(keyStr) != null
    }

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        return defaultFindMember(key.asString().value)
    }

    protected fun defaultFindMember(key: String): SkValue {
        props.get(key)?.let { return it }

        klass.findInstanceMethod(key)?.let { method ->
            val bound = BoundMethod(method, this, SkArguments())
            props = props.withAdded(key, bound)
            return bound
        }

        return SkUndefined
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        defaultDeleteMember(key.asString().value)
    }

    protected fun defaultDeleteMember(key: String) {
        klass.findInstanceMethod(key)?.let {
            throw UnsupportedOperationException("Can't delete method $key")
        }

        props = props.withRemoved(key)
    }

    override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        notSupported("Can't call objects")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        return callMethod("rangeTo", SkArguments.of(end, endInclusive.toSkript()), state, exprDebug)
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue {
        props.get(methodName)?.let { override ->
            return when (override) {
                is SkMethod -> override.call(this, args, state)
                is SkFunction -> override.call(args, state)
                else -> throw UnsupportedOperationException("$exprDebug.$methodName is neither a method nor a function")
            }
        }

        klass.findInstanceMethod(methodName)?.let { method ->
            return method.call(this, args, state)
        }

        throw UnsupportedOperationException("$exprDebug has no method $methodName")
    }
}

object SkObjectClassDef : SkClassDef("Object", null)