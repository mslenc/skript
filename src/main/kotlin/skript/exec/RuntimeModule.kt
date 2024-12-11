package skript.exec

import skript.analysis.ModuleScope
import skript.ast.ParsedModule
import skript.values.SkMap
import skript.values.SkUndefined
import skript.values.SkValue

class RuntimeModule(val name: String, numModuleVars: Int, val init: FunctionDef) {
    val vars = Array<SkValue>(numModuleVars) { SkUndefined }
    val exports = SkMap()

    constructor(module: ParsedModule, moduleScope: ModuleScope, init: FunctionDef) : this(module.name, moduleScope.varsAllocated, init)
}

