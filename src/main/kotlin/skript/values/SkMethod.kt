package skript.values

import skript.exec.RuntimeState
import skript.notSupported

sealed class SkCallable(val name: String) : SkObject() {
    final override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    final override fun asNumber(): SkNumber {
        notSupported("Can't convert functions into numbers")
    }
}

abstract class SkFunction(name: String) : SkCallable(name) {
    override val klass: SkClass
        get() = FunctionClass

    abstract override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue

    final override fun getKind(): SkValueKind {
        return SkValueKind.FUNCTION
    }

    override fun asString(): SkString {
        return SkString("[function $name]")
    }
}

abstract class SkMethod(name: String, val paramNames: List<String>) : SkCallable(name) {
    override val klass: SkClass
        get() = MethodClass

    abstract val expectedClass: SkClass
    protected val nameString by lazy { SkString("function ${expectedClass.name}.$name(${paramNames}) {[ native code ]}") }

    final override fun getKind(): SkValueKind {
        return SkValueKind.METHOD
    }

    override fun asString(): SkString {
        return nameString
    }

    abstract suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue
}

class BoundMethod(val method: SkMethod, val thiz: SkValue, val boundPosArgs: List<SkValue>, val boundKwArgs: Map<String, SkValue>) : SkFunction(method.name + "(bound)") {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val allPosArgs = when {
            boundPosArgs.isEmpty() -> posArgs
            posArgs.isEmpty() -> boundPosArgs
            else -> boundPosArgs + posArgs
        }

        val allKwArgs = when {
            boundKwArgs.isEmpty() -> kwArgs
            kwArgs.isEmpty() -> boundKwArgs
            else -> boundKwArgs + kwArgs
        }

        return method.call(thiz, allPosArgs, allKwArgs, state)
    }
}