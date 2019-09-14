package skript.interop

import skript.io.SkriptEnv
import skript.util.SkArguments
import skript.values.SkList
import skript.values.SkNull
import skript.values.SkUndefined
import kotlin.reflect.KParameter

sealed class SkNativeParam(val kotlinParam: KParameter) {
    abstract val name: String?
    abstract fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv)
}

class SkNativeParamNormal<T>(override val name: String, kotlinParam: KParameter, val codec: SkCodec<T>) : SkNativeParam(kotlinParam) {
    override fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv) {
        when (val value = args.getParam(name)) {
            SkUndefined -> return
            SkNull -> nativeArgs[kotlinParam] = null
            else -> nativeArgs[kotlinParam] = codec.toKotlin(value, env)
        }
    }
}

class SkNativeParamRestArgs<T>(kotlinParam: KParameter, val codec: SkCodec<T>) : SkNativeParam(kotlinParam) {
    override val name: String? get() = null

    override fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, args: SkArguments, env: SkriptEnv) {
        val posArgs = args.getRemainingPosArgs()
        nativeArgs[kotlinParam] = codec.toKotlin(SkList(posArgs), env)
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