package skript.io

enum class ModuleType {
    SKRIPT,
    PAGE_TEMPLATE,
}

data class ModuleSource(val source: String, val moduleName: String, val fileName: String, val type: ModuleType)

interface ModuleSourceProvider {
    suspend fun getSource(moduleName: String): ModuleSource?

    companion object {
        fun combine(providers: List<ModuleSourceProvider>): ModuleSourceProvider {
            return CombinedModuleSourceProvider(providers.toList())
        }

        @JvmName("staticSources")
        fun static(skripts: Map<String, String>, templates: Map<String, String>): ModuleSourceProvider {
            val transformed = LinkedHashMap<String, ModuleSource>()

            skripts.forEach { (moduleName, source) ->
                transformed[moduleName] = ModuleSource(source, moduleName, moduleName, ModuleType.SKRIPT)
            }
            templates.forEach { (moduleName, source) ->
                transformed[moduleName] = ModuleSource(source, moduleName, moduleName, ModuleType.PAGE_TEMPLATE)
            }

            if (transformed.size != skripts.size + templates.size)
                throw IllegalArgumentException("Sources have a repeated key.")

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