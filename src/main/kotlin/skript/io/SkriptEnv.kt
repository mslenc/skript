package skript.io

import com.github.mslenc.utils.error
import com.github.mslenc.utils.getLogger
import skript.analysis.*
import skript.ast.*
import skript.exec.*
import skript.interop.SkCodec
import skript.opcodes.ExecuteSuspend
import skript.opcodes.JumpTarget
import skript.opcodes.ReturnValue
import skript.opcodes.ThrowException
import skript.parser.*
import skript.templates.TemplateInstance
import skript.templates.TemplateRuntime
import skript.util.Globals
import skript.util.SkArguments
import skript.values.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

val anonCounter = AtomicLong()

class SkriptEnv(val engine: SkriptEngine, val moduleProvider: ModuleProvider, val moduleNameResolver: ModuleNameResolver) {
    internal val globals = Globals()
    val modules = HashMap<ModuleName, RuntimeModule>()
    val classes = HashMap<SkClassDef, SkClass>()

    fun getClassObject(classDef: SkClassDef): SkClass {
        val superClass = classDef.superClass?.let { getClassObject(it) }
        return classes.getOrPut(classDef) { SkClass(classDef, superClass) }
    }

    fun <T: Any> createNativeWrapper(value: T, klass: KClass<T> = value::class as KClass<T>): SkValue {
        if (value is SkValue)
            return value

        if (value is List<*>) {
            val parts = value.map { if (it != null) createNativeWrapper(it) else SkNull }
            return SkList(parts)
        }

        if (value is Map<*, *>) {
            return SkMap(value.map { it.key.toString() to (it.value?.let { v -> createNativeWrapper(v) } ?: SkNull) }.toMap())
        }

        val codec = engine.getNativeCodec(klass) ?: throw UnsupportedOperationException("Couldn't reflect class $klass")
        return codec.toSkript(value, this)
    }

    fun <T: Any> setNativeGlobal(name: String, value: T, klass: KClass<T> = value::class as KClass<T>, protected: Boolean = true) {
        val skValue = createNativeWrapper(value, klass)
        setGlobal(name, skValue, protected)
    }

    fun <T: Any> setClassAsGlobal(klass: KClass<T>, name: String = klass.simpleName ?: throw IllegalArgumentException("Need a class name"), protected: Boolean = true) {
        val classDef = engine.getNativeClassDef(klass) ?: throw UnsupportedOperationException("Couldn't reflect class $klass")
        val classObj = getClassObject(classDef)
        setGlobal(name, classObj, protected = protected)
    }

    fun setGlobal(name: String, value: SkValue, protected: Boolean = true) {
        globals.set(name, value, protected)
    }

    fun getGlobal(name: String): SkValue? {
        return globals.get(name).also { if (it == SkUndefined) return null }
    }

    suspend fun registerAndInitModule(moduleName: ModuleName, initializer: FunctionDef): Pair<SkValue, RuntimeModule> {
        val runtimeModule = RuntimeModule(moduleName, SkMap())
        modules[moduleName] = runtimeModule
        return Pair(executeFunction(initializer, emptyArray(), SkArguments()), runtimeModule)
    }

    fun registerPreInitializedModule(moduleName: ModuleName, moduleExports: SkMap): RuntimeModule {
        val runtimeModule = RuntimeModule(moduleName, moduleExports)
        modules[moduleName] = runtimeModule
        return runtimeModule
    }

    private fun failImport(moduleName: ModuleName, sourceName: String, pos: Pos?): Nothing {
        val extraMsg = if (sourceName != moduleName.name) " (resolved to \"${ moduleName.name }\")" else ""
        throw SkImportError("Couldn't read module $sourceName$extraMsg", null, pos)
    }

    /* This guy is for calling from scripts */
    internal suspend fun requireModule(sourceName: String, importingModule: ModuleName, pos: Pos?): RuntimeModule {
        val moduleName = moduleNameResolver.resolve(sourceName, importingModule)

        try {
            getOrInitModule(moduleName)?.let { return it }
        } catch (e: Exception) {
            logger.error { "Failed to import $sourceName from $importingModule at $pos" }
            // fail below
        }

        failImport(moduleName, sourceName, pos)
    }

    internal suspend fun getOrInitModule(moduleName: ModuleName): RuntimeModule? {
        modules[moduleName]?.let { return it }

        val module = moduleProvider.findModule(moduleName, engine) ?: return null

        return module.instantiate(this).second
    }

    internal suspend fun runAnonModule(prepared: PreparedModuleSkript): SkValue {
        val runtimeModule = RuntimeModule(prepared.moduleName, SkMap())
        modules[prepared.moduleName] = runtimeModule
        try {
            return executeFunction(prepared.moduleInit, emptyArray(), SkArguments())
        } finally {
            modules.remove(prepared.moduleName)
        }
    }

    suspend fun runAnonymousScript(scriptSource: String): SkValue {
        val moduleName = "<anonModule${ anonCounter.incrementAndGet() }>"
        val source = ModuleSourceSkript(ModuleName(moduleName), scriptSource)
        return runAnonModule(source.prepare(engine))
    }

    private fun makeWrapperForRender(render: SkFunction): TemplateInstance {
        return object : TemplateInstance {
            override suspend fun execute(ctx: SkMap, out: TemplateRuntime) {
                val args = SkArguments()
                args.addPosArg(ctx)
                args.addPosArg(createNativeWrapper(out))

                render.call(args, this@SkriptEnv)
            }
        }
    }

    suspend fun runAnonymousTemplate(templateSource: String, ctx: Map<String, Any?>, runtime: TemplateRuntime) {
        val skCtx = SkMap(ctx.mapValues { (_, v) -> v?.let { createNativeWrapper(v) } ?: SkNull })
        runAnonymousTemplate(templateSource, skCtx, runtime)
    }

    suspend fun runAnonymousTemplate(templateSource: String, ctx: SkMap, runtime: TemplateRuntime) {
        val moduleName = "<anonTemplate${ anonCounter.incrementAndGet() }>"
        val source = ModuleSourceTemplate(ModuleName(moduleName), templateSource)
        val module = source.prepare(engine)

        val exports = SkMap()
        val runtimeModule = RuntimeModule(module.moduleName, exports)
        modules[module.moduleName] = runtimeModule
        try {
            executeFunction(module.moduleInit, emptyArray(), SkArguments())

            val render = exports.entryGet("render".toSkript(), this) as SkFunction
            val wrapper = makeWrapperForRender(render)
            wrapper.execute(ctx, runtime)
        } finally {
            modules.remove(module.moduleName)
        }
    }

    suspend fun createFunction(paramNames: List<String>, functionBody: String): SkScriptFunction {
        val funcName = "<callable${ anonCounter.incrementAndGet() }>"
        val moduleName = ModuleName("<anonModule${ anonCounter.incrementAndGet() }>")

        val tokens = CharStream(functionBody, funcName).lexCodeModule()
        val funcBody = ModuleParser(Tokens(tokens)).parseStatements(TokenType.EOF, allowFunctions = true, allowClasses = false, allowVars = true)
        val funcDecl = DeclareFunction(funcName, paramNames.map { ParamDecl(it, ParamType.NORMAL, null) }, Statements(funcBody), Pos(1, 1, "generated"), export = false)
        val returnStmt = ReturnStatement(Variable(funcName, Pos(1, 1, "generated")))
        val initializer = listOf(funcDecl, returnStmt)
        val moduleScope = VarAllocator(moduleName).visitModule(initializer)
        val moduleInit = OpCodeGen(moduleName).visitModule(moduleScope, initializer)
        val preparedModule = PreparedModuleSkript(moduleName, moduleInit)

        return runAnonModule(preparedModule) as SkScriptFunction
    }

    suspend fun <T> createCallable(params: List<Pair<String, SkCodec<*>>>, returnType: SkCodec<T>, functionBody: String): SuspendFun<T> {
        val function = createFunction(params.map { it.first }, functionBody)
        return SuspendFunImpl(params, returnType, function, this)
    }

    suspend fun executeFunction(func: FunctionDef, closure: Array<Array<SkValue>>, args: SkArguments): SkValue {
        val ops = func.ops
        val opsSize = ops.size

        val frame = Frame(func.localsSize, args, closure, this)
        var ip = 0

        nextOp@
        while (ip < opsSize) {
            val op = ops[ip++]
            var result = op.execute(frame)
            if (result === ExecuteSuspend)
                result = op.executeSuspend(frame)

            when (result) {
                null -> continue@nextOp
                is JumpTarget -> ip = result.value
                is ReturnValue -> return result.result
                is ThrowException -> throw result.ex
                is ExecuteSuspend -> throw IllegalStateException("executeSuspend() returned ExecuteSuspend")
            }
        }

        return SkUndefined
    }

    fun getModuleExports(moduleName: ModuleName): SkMap {
        return modules[moduleName]?.exports ?: throw IllegalStateException("No module $moduleName in runtime")
    }

    suspend fun loadTemplate(sourceName: String): TemplateInstance {
        return loadTemplate(moduleNameResolver.resolve(sourceName, null))
    }

    suspend fun loadTemplate(moduleName: ModuleName): TemplateInstance {
        val module = getOrInitModule(moduleName) ?: throw IllegalStateException("Template module $moduleName couldn't be loaded.")
        val render = module.exports.entries["render"] as? SkFunction ?: throw IllegalStateException("No render function in module $moduleName.")
        return makeWrapperForRender(render)
    }

    val logger = getLogger<SkriptEnv>()
}

interface SuspendFun<T> {
    suspend operator fun invoke(vararg args: Any?): T
}

internal class SuspendFunImpl<T>(val params: List<Pair<String, SkCodec<*>>>, val retCodec: SkCodec<T>, val function: SkFunction, val env: SkriptEnv) : SuspendFun<T> {
    override suspend fun invoke(vararg args: Any?): T {
        val skArgs = SkArguments()

        for (i in params.indices) {
            val arg = args.getOrNull(i)

            when {
                i >= args.size -> skArgs.addPosArg(SkUndefined)
                arg == null -> skArgs.addPosArg(SkNull)
                else -> skArgs.addPosArg(doImport(params[i].second, arg))
            }
        }

        val skResult = function.call(skArgs, env)
        return retCodec.toKotlin(skResult, env)
    }

    private fun <T> doImport(codec: SkCodec<T>, value: Any): SkValue {
        return codec.toSkript(value as T, env)
    }
}