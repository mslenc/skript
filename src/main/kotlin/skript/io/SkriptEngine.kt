package skript.io

import skript.values.*

class SkriptEngine(val moduleProvider: ParsedModuleProvider) {
    fun createEnv(initStandardGlobals: Boolean = true): SkriptEnv {
        val env = SkriptEnv(this)

        if (initStandardGlobals) {
            env.apply {
                setGlobal("String", StringClass, true)
                setGlobal("Number", NumberClass, true)
                setGlobal("Boolean", BooleanClass, true)
                setGlobal("Object", ObjectClass, true)
                setGlobal("List", ListClass, true)
                setGlobal("Map", ListClass, true)
            }
        }

        return env
    }
}