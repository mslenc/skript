package skript.values

import skript.exec.FunctionDef
import skript.exec.RuntimeState
import skript.util.SkArguments

class SkScriptFunction(private val def: FunctionDef, private val closure: Array<Array<SkValue>>) : SkFunction(def.name, def.paramDefs.map { it.name }) {
    override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        return state.executeFunction(def, closure, args)
    }
}