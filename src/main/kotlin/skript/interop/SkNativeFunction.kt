package skript.interop

import skript.exec.ParamType
import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.util.ArgsExtractor
import skript.values.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy

class ParamInfo<T>(
    val name: String,
    val kotlinParam: KParameter,
    val paramType: ParamType,
    val codec: SkCodec<T>
) {
    suspend fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, instance: Any?, args: ArgsExtractor, env: SkriptEnv) {
        nativeArgs[kotlinParam] = when (kotlinParam.kind) {
            KParameter.Kind.EXTENSION_RECEIVER, // TODO is this ok, just using the instance as receiver like this?
            KParameter.Kind.INSTANCE -> {
                instance
            }
            KParameter.Kind.VALUE -> {
                val value = when(paramType) {
                    ParamType.NORMAL -> {
                        args.extractParam(name)
                    }

                    ParamType.POS_ARGS -> {
                        SkList(args.getRemainingPosArgs())
                    }

                    ParamType.KW_ARGS -> {
                        SkMap(args.getRemainingKwArgs())
                    }
                }

                when (value) {
                    SkUndefined -> return // so that we might use native defaults
                    SkNull -> null
                    else -> codec.toKotlin(value, env)
                }
            }
        }
    }
}

class SkNativeFunction<T>(name: String, val params: List<ParamInfo<*>>, val impl: KFunction<*>, val resultCodec: SkCodec<T>) : SkFunction(name, params.map { it.name }) {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val env = state.env
        val args = ArgsExtractor(posArgs, kwArgs, name)
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, null, args, env) }

        val result = if (impl.isSuspend) {
            impl.callSuspendBy(nativeArgs)
        } else {
            impl.callBy(nativeArgs)
        } as T?

        if (result == null)
            return SkNull

        return resultCodec.toSkript(result, state.env)
    }
}

class SkNativeConstructor<T : Any>(name: String, val params: List<ParamInfo<*>>, val impl: KFunction<T>, val skClass: SkNativeClassDef<T>) : SkFunction(name, params.map { it.name }) {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkNativeObject<T> {
        return call(posArgs, kwArgs, state.env)
    }

    suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, env: SkriptEnv): SkNativeObject<T> {
        val args = ArgsExtractor(posArgs, kwArgs, name)
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, null, args, env) }

        val nativeObject = if (impl.isSuspend) {
            impl.callSuspendBy(nativeArgs)
        } else {
            impl.callBy(nativeArgs)
        }

        return SkNativeObject(nativeObject, skClass)
    }
}