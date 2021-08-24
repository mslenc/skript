package skript.values

import skript.interop.SkClassInstanceMember
import skript.interop.SkClassStaticMember
import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments

sealed class SkCallable(val name: String) : SkObject() {
    final override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    final override fun asNumber(): SkNumber {
        typeError("Can't convert functions into numbers")
    }

    override fun unwrap(): SkCallable {
        return this
    }
}

abstract class SkFunction(name: String, val paramNames: List<String>) : SkCallable(name), SkClassStaticMember {
    override val klass: SkClassDef
        get() = SkFunctionClassDef

    abstract override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue

    final override fun getKind(): SkValueKind {
        return SkValueKind.FUNCTION
    }

    override fun asString(): SkString {
        return SkString("[function $name]")
    }
}

abstract class SkMethod(name: String, val paramNames: List<String>) : SkCallable(name), SkClassInstanceMember {
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

    abstract suspend fun call(thiz: SkValue, args: SkArguments, env: SkriptEnv): SkValue
}

class BoundMethod(private val method: SkMethod, private val thiz: SkValue, boundArgs: SkArguments) : SkFunction(method.name + "(bound)", method.paramNames) {
    private val boundPosArgs = boundArgs.extractAllPosArgs()
    private val boundKwArgs = boundArgs.extractAllKwArgs()

    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val combinedArgs = if (boundPosArgs.isEmpty() && boundKwArgs.isEmpty()) {
            args
        } else {
            SkArguments().apply {
                spreadPosArgs(boundPosArgs)
                spreadKwArgs(boundKwArgs)
                spreadPosArgs(args.extractAllPosArgs())
                spreadKwArgs(args.extractAllKwArgs())
            }
        }

        return method.call(thiz, combinedArgs, env)
    }
}

object SkFunctionClassDef : SkClassDef("Function")

object SkMethodClassDef : SkClassDef("Method")