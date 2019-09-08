package skript.io

import skript.analysis.*
import skript.ast.Module
import skript.exec.RuntimeModule
import skript.exec.RuntimeState
import skript.util.Globals
import skript.values.SkClass
import skript.values.SkClassDef
import skript.values.SkUndefined
import skript.values.SkValue
import java.util.concurrent.atomic.AtomicLong

val anonCounter = AtomicLong()

class SkriptEnv(val engine: SkriptEngine) {
    internal val globals = Globals()
    val globalScope = GlobalScope()
    val modules = HashMap<String, RuntimeModule>()
    val classes = HashMap<SkClassDef, SkClass>()

    fun getClassObject(classDef: SkClassDef): SkClass {
        val superClass = classDef.superClass?.let { getClassObject(it) }
        return classes.getOrElse(classDef) { SkClass(classDef, superClass) }
    }

    fun setGlobal(name: String, value: SkValue, protected: Boolean = true) {
        globals.set(name, value, protected)
    }

    fun getGlobal(name: String): SkValue? {
        return globals.get(name).also { if (it == SkUndefined) return null }
    }

    internal fun analyze(module: Module) {
        VarAllocator(globalScope).visitModule(module)
        OpCodeGen().visitModule(module)
    }

    suspend fun runAnonymousScript(scriptSource: String): SkValue {
        val moduleName = "<anonModule${ anonCounter.incrementAndGet() }>"
        val source = ModuleSource(scriptSource, moduleName, moduleName)
        val parsedModule = source.parse()

        analyze(parsedModule)

        val runtimeModule = RuntimeModule(moduleName, parsedModule.moduleScope.varsAllocated)
        modules[moduleName] = runtimeModule
        try {
            return RuntimeState(globals, this).executeFunction(parsedModule.moduleInit, emptyArray(), emptyList(), emptyMap())
        } finally {
            modules.remove(moduleName)
        }
    }
}