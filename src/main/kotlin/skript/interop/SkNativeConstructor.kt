package skript.interop

import skript.io.SkriptEnv
import skript.util.SkArguments
import skript.values.SkFunction
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class SkNativeConstructor<T : Any>(name: String, val params: SkNativeParams, val impl: KFunction<T>, val skClass: SkNativeClassDef<T>) : SkFunction(name, params.mapNotNull { it.name }) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkNativeObject<T> {
        return fastCall(args, env)
    }

    fun fastCall(args: SkArguments, env: SkriptEnv): SkNativeObject<T> {
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, args, env) }

        val nativeObject = impl.callBy(nativeArgs)

        return SkNativeObject(nativeObject, skClass)
    }
}