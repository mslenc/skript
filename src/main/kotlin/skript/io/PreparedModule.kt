package skript.io

import skript.exec.RuntimeModule
import skript.values.SkValue

interface PreparedModule {
    val moduleName: ModuleName
    suspend fun instantiate(env: SkriptEnv): Pair<SkValue, RuntimeModule>
}