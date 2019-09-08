package skript.values

import skript.exec.Frame
import skript.exec.FunctionDef
import skript.exec.RuntimeState

class SkScriptFunction(private val def: FunctionDef, private val closure: Array<Frame>) : SkFunction(def.name, def.paramDefs.map { it.name }) {
    override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        return state.executeFunction(def, closure, posArgs, kwArgs)
    }
}