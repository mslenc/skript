package skript.interop

import skript.exec.RuntimeState
import skript.util.ArgsExtractor
import skript.values.SkClass
import skript.values.SkMethod
import skript.values.SkValue
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy

class SkNativeMethod(name: String, val thisParam: KParameter, val params: List<ParamInfo<*>>, val impl: KFunction<*>, override val expectedClass: SkClass) : SkMethod(name, params.map { it.name }) {
    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val kotlinThis = (thiz as SkNativeObject<*>).nativeObj

        val args = ArgsExtractor(posArgs, kwArgs, name)
        val nativeArgs = HashMap<KParameter, Any?>()
        params.forEach { it.doImportInto(nativeArgs, null, args) }
        nativeArgs[thisParam] = kotlinThis

        return if (impl.isSuspend) {
            state.importKotlinValue(impl.callSuspendBy(nativeArgs))
        } else {
            state.importKotlinValue(impl.callBy(nativeArgs))
        }
    }
}