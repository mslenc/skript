package skript.interop

import skript.io.SkriptEnv
import skript.util.SkArguments
import skript.values.SkFunction
import skript.values.SkNull
import skript.values.SkValue
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy


class SkNativeFunction<T>(name: String, val params: SkNativeParams, val impl: KFunction<*>, val resultCodec: SkCodec<T>) : SkFunction(name, params.mapNotNull { it.name }) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, args, env) }

        val result = if (impl.isSuspend) {
            impl.callSuspendBy(nativeArgs)
        } else {
            impl.callBy(nativeArgs)
        } as T?

        if (result == null)
            return SkNull

        return resultCodec.toSkript(result, env)
    }
}

