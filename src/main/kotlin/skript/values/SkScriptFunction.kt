package skript.values

import skript.exec.FunctionDef
import skript.io.SkriptEnv
import skript.util.SkArguments

class SkScriptFunction(private val def: FunctionDef, private val closure: Array<Array<SkValue>>) : SkFunction(def.name, def.paramDefs.map { it.name }) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        return env.executeFunction(def, closure, args)
    }
}