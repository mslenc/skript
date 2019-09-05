package skript.exec

import skript.values.SkMap
import skript.values.SkUndefined
import skript.values.SkValue

class RuntimeModule(val name: String, numModuleVars: Int) {
    val vars = Array<SkValue>(numModuleVars) { SkUndefined }
    val exports = SkMap()
}

