package skript.interop

import skript.io.SkriptEnv
import skript.util.SkArguments
import skript.values.SkNull
import skript.values.SkUndefined
import kotlin.reflect.KParameter

sealed class SkNativeParam(val kotlinParam: KParameter) {
    abstract val name: String?
    abstract fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv)

    val supplyNullWhenUndefined: Boolean
        get() = !kotlinParam.isOptional && kotlinParam.type.isMarkedNullable
}

class SkNativeParamNormal<T>(override val name: String, kotlinParam: KParameter, val codec: SkCodec<T>) : SkNativeParam(kotlinParam) {

    override fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv) {
        nativeArgs[kotlinParam] = when (val value = args.extractArg(name)) {
            SkUndefined -> if (supplyNullWhenUndefined) null else return
            SkNull -> null
            else -> codec.toKotlin(value, env)
        }
    }
}

class SkNativeParamKwOnly<T>(override val name: String, kotlinParam: KParameter, val codec: SkCodec<T>) : SkNativeParam(kotlinParam) {
    override fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv) {
        nativeArgs[kotlinParam] = when (val value = args.extractKwOnlyArg(name)) {
            SkUndefined -> if (supplyNullWhenUndefined) null else return
            SkNull -> null
            else -> codec.toKotlin(value, env)
        }
    }
}

class SkNativeParamRestArgs<T>(override val name: String, kotlinParam: KParameter, val codec: SkCodec<T>) : SkNativeParam(kotlinParam) {
    override fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv) {
        val posArgs = args.extractPosVarArgs(name)
        nativeArgs[kotlinParam] = codec.toKotlin(posArgs, env)
    }
}

class SkNativeParamConst(kotlinParam: KParameter, val instance: Any) : SkNativeParam(kotlinParam) {
    override val name: String? get() = null

    override fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv) {
        nativeArgs[kotlinParam] = instance
    }
}

class SkNativeParams(val params: List<SkNativeParam>) : List<SkNativeParam> by params {

}