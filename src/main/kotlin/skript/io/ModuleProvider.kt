package skript.io

interface ModuleProvider {
    /**
     * Should return module, if available (null otherwise). For modules based on skript code or page templates,
     * the expected method is to implement a ModuleSourceProvider instead, wrapping it with [ModuleProvider.Companion.from].
     *
     * @param moduleName the moduleName, as previously obtained from [resolveModuleName].
     * @param engine the engine the module will be used in
     */
    suspend fun findModule(moduleName: ModuleName, engine: SkriptEngine): PreparedModule?

    companion object {
        fun from(sourceProvider: ModuleSourceProvider): ModuleProvider {
            return SourcedModuleProvider(sourceProvider)
        }

        fun from(sourceProviders: List<ModuleSourceProvider>): ModuleProvider {
            return combine(sourceProviders.map { from(it) })
        }

        fun combine(providers: List<ModuleProvider>): ModuleProvider {
            return when (providers.size) {
                0 -> NoModuleProvider
                1 -> providers.single()
                else -> CombiningProvider(providers.toList())
            }
        }
    }
}


object NoModuleProvider : ModuleProvider {
    override suspend fun findModule(moduleName: ModuleName, engine: SkriptEngine): PreparedModule? {
        return null
    }
}

internal class SourcedModuleProvider(val sourceProvider: ModuleSourceProvider) : ModuleProvider {
    override suspend fun findModule(moduleName: ModuleName, engine: SkriptEngine): PreparedModule? {
        return sourceProvider.findSource(moduleName)?.prepare(engine)
    }
}

internal class CombiningProvider(val providers: List<ModuleProvider>): ModuleProvider {
    override suspend fun findModule(moduleName: ModuleName, engine: SkriptEngine): PreparedModule? {
        for (provider in providers)
            provider.findModule(moduleName, engine)?.let { return it }
        return null
    }
}