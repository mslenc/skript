package skript.io

import skript.analysis.*
import skript.ast.*
import skript.exec.*
import skript.interop.SkCodec
import skript.opcodes.ExecuteSuspend
import skript.opcodes.JumpTarget
import skript.opcodes.ReturnValue
import skript.opcodes.ThrowException
import skript.parser.*
import skript.templates.TemplateRuntime
import skript.util.Globals
import skript.util.SkArguments
import skript.values.*
import java.io.StringWriter
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

val anonCounter = AtomicLong()

class SkriptEnv(val engine: SkriptEngine) {
    internal val globals = Globals()
    val globalScope = GlobalScope()
    val modules = HashMap<String, RuntimeModule>()
    val classes = HashMap<SkClassDef, SkClass>()

    fun setUpForTemplate(receiver: Appendable, defaultEscapeKey: String = "raw", locale: Locale = Locale.US, timeZone: ZoneId = ZoneId.systemDefault(), currency: Currency = Currency.getInstance(locale)) {
        val runtime = TemplateRuntime.createWithDefaults(receiver, defaultEscapeKey, locale, timeZone, currency)
        setNativeGlobal("templateRuntime", runtime)
    }

    fun getClassObject(classDef: SkClassDef): SkClass {
        val superClass = classDef.superClass?.let { getClassObject(it) }
        return classes.getOrPut(classDef) { SkClass(classDef, superClass) }
    }

    fun <T: Any> createNativeWrapper(value: T, klass: KClass<T> = value::class as KClass<T>): SkValue {
        if (value is SkValue)
            return value

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

    internal fun analyze(module: Module) {
        VarAllocator(globalScope).visitModule(module)
        OpCodeGen().visitModule(module)
    }

    suspend fun runAnonymousScript(scriptSource: String): SkValue {
        val moduleName = "<anonModule${ anonCounter.incrementAndGet() }>"
        val source = ModuleSource(scriptSource, moduleName, moduleName, ModuleType.SKRIPT)
        val module = source.parse()

        analyze(module)

        val runtimeModule = RuntimeModule(module)
        modules[module.name] = runtimeModule
        return executeFunction(module.moduleInit, emptyArray(), SkArguments())
    }

    suspend fun runAnonymousTemplate(templateSource: String, defaultEscapeKey: String = "raw", locale: Locale = Locale.US, timeZone: ZoneId = ZoneId.systemDefault(), currency: Currency = Currency.getInstance(locale)): String {
        val out = StringBuilder()
        setUpForTemplate(out, defaultEscapeKey, locale, timeZone, currency)

        val moduleName = "<anonTemplate${ anonCounter.incrementAndGet() }>"
        val source = ModuleSource(templateSource, moduleName, moduleName, ModuleType.PAGE_TEMPLATE)
        val module = source.parse()

        analyze(module)

        val runtimeModule = RuntimeModule(module)
        modules[module.name] = runtimeModule
        executeFunction(module.moduleInit, emptyArray(), SkArguments())

        return out.toString()
    }

    suspend fun createFunction(paramNames: List<String>, functionBody: String): SkScriptFunction {
        val funcName = "<callable${ anonCounter.incrementAndGet() }>"
        val moduleName = "<anonModule${ anonCounter.incrementAndGet() }>"

        val tokens = CharStream(functionBody, funcName).lexCodeModule()
        val funcBody = ModuleParser(Tokens(tokens)).parseStatements(TokenType.EOF, allowFunctions = true, allowClasses = false, allowVars = true)
        val funcDecl = DeclareFunction(funcName, paramNames.map { ParamDecl(it, ParamType.NORMAL, null) }, Statements(funcBody), Pos(1, 1, "generated"))
        val returnFunc = ReturnStatement(Variable(funcName, Pos(1, 1, "generated")))
        val module = Module(moduleName, listOf(funcDecl, returnFunc))

        analyze(module)

        val runtimeModule = RuntimeModule(module)
        val theFunction: SkScriptFunction
        modules[module.name] = runtimeModule
        try {
            theFunction = executeFunction(module.moduleInit, emptyArray(), SkArguments()) as SkScriptFunction
        } finally {
            modules.remove(module.name)
        }

        return theFunction
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