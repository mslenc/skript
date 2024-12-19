package skript.io

abstract class ModuleSource {
    abstract val moduleName: ModuleName

    abstract fun prepare(engine: SkriptEngine): PreparedModule
}