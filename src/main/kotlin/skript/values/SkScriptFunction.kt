package skript.values

import skript.exec.Frame
import skript.exec.FunctionDef
import skript.exec.ParamType
import skript.exec.RuntimeState
import skript.util.ArgsExtractor

class SkScriptFunction(private val def: FunctionDef, private val closure: Array<Frame>) : SkFunction(def.name) {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        val frame = state.startScriptFrame(def.ops, def.localsSize, closure)

        val args = ArgsExtractor(posArgs, kwArgs, def.name)
        for (param in def.paramDefs) {
            frame.locals[param.localIndex] = when (param.type) {
                ParamType.NORMAL -> args.extractParam(param.name)
                ParamType.POS_ARGS -> SkList(args.getRemainingPosArgs())
                ParamType.KW_ARGS -> SkMap(args.getRemainingKwArgs())
            }
        }

        return state.execute()
    }
}