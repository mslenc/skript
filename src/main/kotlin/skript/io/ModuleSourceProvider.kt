package skript.io

interface ModuleSourceProvider {
    /**
     * Returns the module source of the specified module (or null, if not found).
     */
    suspend fun findSource(moduleName: ModuleName): ModuleSource?

    companion object {
        fun combine(providers: List<ModuleSourceProvider>): ModuleSourceProvider {
            return CombinedModuleSourceProvider(providers.toList())
        }

        @JvmName("staticSources")
        fun static(skripts: Map<String, String>, templates: Map<String, String>): ModuleSourceProvider {
            val transformed = LinkedHashMap<ModuleName, ModuleSource>()

            skripts.forEach { (name, source) ->
                ModuleName(name).let { transformed[it] = ModuleSourceSkript(it, source) }
            }
            templates.forEach { (name, source) ->
                ModuleName(name).let { transformed[it] = ModuleSourceTemplate(it, source) }
            }

            if (transformed.size != skripts.size + templates.size)
                throw IllegalArgumentException("Sources have a repeated key.")

            return static(transformed)
        }

        @JvmName("staticByString")
        fun static(sources: Map<String, ModuleSource>): ModuleSourceProvider {
            return StaticModuleSourceProvider(sources.mapKeys { ModuleName(it.key) })
        }

        @JvmName("staticByName")
        fun static(sources: Map<ModuleName, ModuleSource>): ModuleSourceProvider {
            return StaticModuleSourceProvider(sources)
        }
    }
}


internal class CombinedModuleSourceProvider(private val sources: List<ModuleSourceProvider>) : ModuleSourceProvider {
    override suspend fun findSource(moduleName: ModuleName): ModuleSource? {
        for (source in sources) {
            source.findSource(moduleName)?.let { return it }
        }
        return null
    }
}

internal class StaticModuleSourceProvider(private val sources: Map<ModuleName, ModuleSource>) : ModuleSourceProvider {
    override suspend fun findSource(moduleName: ModuleName): ModuleSource? {
        return sources[moduleName]
    }
}