package skript.io

import skript.values.*

class SkriptEngine(val moduleProvider: ParsedModuleProvider) {
    fun createEnv(initStandardGlobals: Boolean = true): SkriptEnv {
        val env = SkriptEnv(this)

        if (initStandardGlobals) {
            env.apply {
                setGlobal("String", env.getClassObject(SkStringClassDef), true)
                setGlobal("Number", env.getClassObject(SkNumberClassDef), true)
                setGlobal("Boolean", env.getClassObject(SkBooleanClassDef), true)
                setGlobal("Object", env.getClassObject(SkObjectClassDef), true)
                setGlobal("List", env.getClassObject(SkListClassDef), true)
                setGlobal("Map", env.getClassObject(SkMapClassDef), true)
            }
        }

        return env
    }
}