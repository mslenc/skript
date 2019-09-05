package skript.io

data class ModuleSource(val source: String, val moduleName: String, val fileName: String)

interface ModuleSourceProvider {
    suspend fun getSource(moduleName: String): ModuleSource?

    companion object {
        fun combine(providers: List<ModuleSourceProvider>): ModuleSourceProvider {
            return CombinedModuleSourceProvider(providers.toList())
        }

        @JvmName("staticSources")
        fun static(sources: Map<String, String>): ModuleSourceProvider {
            val transformed = LinkedHashMap<String, ModuleSource>()
            sources.forEach { moduleName, source ->
                transformed[moduleName] = ModuleSource(source, moduleName, moduleName)
            }

            return StaticModuleSourceProvider(transformed)
        }

        @JvmName("staticInfos")
        fun static(sources: Map<String, ModuleSource>): ModuleSourceProvider {
            return StaticModuleSourceProvider(sources.toMap())
        }
    }
}

internal class CombinedModuleSourceProvider(private val sources: List<ModuleSourceProvider>) : ModuleSourceProvider {
    override suspend fun getSource(moduleName: String): ModuleSource? {
        for (source in sources) {
            source.getSource(moduleName)?.let { return it }
        }
        return null
    }
}

internal class StaticModuleSourceProvider(private val sources: Map<String, ModuleSource>) : ModuleSourceProvider {
    override suspend fun getSource(moduleName: String): ModuleSource? {
        return sources[moduleName]
    }
}