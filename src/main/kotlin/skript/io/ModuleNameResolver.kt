package skript.io

/**
 * This represents the resolved name of a module. It may or may not be different from the name used for the module
 * in source (the one in `import * from "moduleName"`), depending on the resolver installed. The resolved name will
 * be used for obtaining sources from ModuleSourceProviders and to determine which imports belong to the same module.
 */
data class ModuleName(val name: String)

interface ModuleNameResolver {
    /**
     * Should resolve the imported module name into a form which:
     * * will be usable by the installed source/module providers
     * * will uniquely identify modules
     */
    fun resolve(sourceName: String, importingModule: ModuleName?): ModuleName
}

object NoNameResolver : ModuleNameResolver {
    override fun resolve(sourceName: String, importingModule: ModuleName?): ModuleName {
        return ModuleName(sourceName)
    }
}
