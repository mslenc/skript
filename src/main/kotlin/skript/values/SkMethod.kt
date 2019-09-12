package skript.values

import skript.exec.RuntimeState
import skript.notSupported
import skript.util.SkArguments

sealed class SkCallable(val name: String) : SkObject() {
    final override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    final override fun asNumber(): SkNumber {
        notSupported("Can't convert functions into numbers")
    }
}

abstract class SkFunction(name: String, val paramNames: List<String>) : SkCallable(name) {
    override val klass: SkClassDef
        get() = SkFunctionClassDef

    abstract override suspend fun call(args: SkArguments, state: RuntimeState): SkValue

    final override fun getKind(): SkValueKind {
        return SkValueKind.FUNCTION
    }

    override fun asString(): SkString {
        return SkString("[function $name]")
    }
}

abstract class SkMethod(name: String, val paramNames: List<String>) : SkCallable(name) {
    override val klass: SkClassDef
        get() = SkMethodClassDef

    abstract val expectedClass: SkClassDef
    protected val nameString by lazy { SkString("function ${expectedClass.className}.$name(${paramNames}) {[ native code ]}") }

    final override fun getKind(): SkValueKind {
        return SkValueKind.METHOD
    }

    override fun asString(): SkString {
        return nameString
    }

    abstract suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue
}

class BoundMethod(private val method: SkMethod, private val thiz: SkValue, boundArgs: SkArguments) : SkFunction(method.name + "(bound)", method.paramNames) {
    private val boundPosArgs = boundArgs.getRemainingPosArgs()
    private val boundKwArgs = boundArgs.getRemainingKwArgs()

    override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        val combinedArgs = if (boundPosArgs.isEmpty() && boundKwArgs.isEmpty()) {
            args
        } else {
            SkArguments().apply {
                spreadPosArgs(boundPosArgs)
                spreadKwArgs(boundKwArgs)
                spreadPosArgs(args.getRemainingPosArgs())
                spreadKwArgs(args.getRemainingKwArgs())
            }
        }

        return method.call(thiz, combinedArgs, state)
    }
}

object SkFunctionClassDef : SkClassDef("Function")

object SkMethodClassDef : SkClassDef("Method")