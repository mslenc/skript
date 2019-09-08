package skript.interop

import skript.exec.ParamType
import skript.exec.RuntimeState
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
    fun doImportInto(nativeArgs: MutableMap<KParameter, Any?>, instance: Any?, args: ArgsExtractor) {
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
                    else -> codec.toKotlin(value)
                }
            }
        }
    }
}

class SkNativeFunction(name: String, val params: List<ParamInfo<*>>, val impl: KFunction<*>) : SkFunction(name, params.map { it.name }) {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val args = ArgsExtractor(posArgs, kwArgs, name)
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, null, args) }

        return if (impl.isSuspend) {
            state.importKotlinValue(impl.callSuspendBy(nativeArgs))
        } else {
            state.importKotlinValue(impl.callBy(nativeArgs))
        }
    }
}

class SkNativeConstructor<T>(name: String, val params: List<ParamInfo<*>>, val impl: KFunction<T>, val skClass: SkNativeClass<T>) : SkFunction(name, params.map { it.name }) {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val args = ArgsExtractor(posArgs, kwArgs, name)
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, null, args) }

        val nativeObject = if (impl.isSuspend) {
            impl.callSuspendBy(nativeArgs)
        } else {
            impl.callBy(nativeArgs)
        }

        return SkNativeObject(nativeObject, skClass)
    }
}