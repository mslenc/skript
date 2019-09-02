package skript.io

interface ModuleSourceProvider {
    fun getSource(moduleName: String): String?
}