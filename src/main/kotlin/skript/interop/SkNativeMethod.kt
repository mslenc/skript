package skript.interop

import skript.exec.RuntimeState
import skript.util.SkArguments
import skript.values.SkClassDef
import skript.values.SkMethod
import skript.values.SkNull
import skript.values.SkValue
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy

class SkNativeMethod<T>(name: String, val thisParam: KParameter, val params: List<ParamInfo<*>>, val resultCodec: SkCodec<T>, val impl: KFunction<*>, override val expectedClass: SkClassDef) : SkMethod(name, params.map { it.name }) {
    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        val kotlinThis = (thiz as SkNativeObject<*>).nativeObj

        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, null, args, state.env) }
        nativeArgs[thisParam] = kotlinThis

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