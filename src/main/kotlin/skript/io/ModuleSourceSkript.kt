package skript.io

import skript.analysis.OpCodeGen
import skript.analysis.VarAllocator
import skript.ast.Statement
import skript.exec.FunctionDef
import skript.exec.RuntimeModule
import skript.parser.CharStream
import skript.parser.ModuleParser
import skript.parser.Tokens
import skript.parser.lexCodeModule
import skript.values.SkValue

data class ModuleSourceSkript(override val moduleName: ModuleName, val source: String, val fileName: String = moduleName.name) : ModuleSource() {
    internal fun parse(): List<Statement> {
        val tokens = Tokens(CharStream(source, fileName).lexCodeModule())
        return ModuleParser(tokens).parseModule()
    }

    override fun prepare(engine: SkriptEngine): PreparedModuleSkript {
        return prepare()
    }

    fun prepare(): PreparedModuleSkript {
        val parsed = parse()
        val moduleScope = VarAllocator(moduleName).visitModule(parsed)
        val moduleInit = OpCodeGen(moduleName).visitModule(moduleScope, parsed)

        return PreparedModuleSkript(moduleName, moduleScope.varsAllocated, moduleInit)
    }
}

data class PreparedModuleSkript(override val moduleName: ModuleName, val varsAllocated: Int, val moduleInit: FunctionDef): PreparedModule {
    override suspend fun instantiate(env: SkriptEnv): Pair<SkValue, RuntimeModule> {
        return env.registerAndInitModule(moduleName, varsAllocated, moduleInit)
    }
}