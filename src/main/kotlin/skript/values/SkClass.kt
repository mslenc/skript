package skript.values

abstract class SkClass(val name: String, val superClass: SkClass?) : SkObject(ClassClass) {
    abstract suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue

    private val instanceMethods = HashMap<String, SkMethod>()
    private val staticFunctions = HashMap<String, SkFunction>()

    final override fun getKind(): SkValueKind {
        return SkValueKind.CLASS
    }

    final override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    fun defineInstanceMethod(method: SkMethod, name: String = method.name) {
        check(!instanceMethods.containsKey(name)) { "This class already has a method named $name" }
        check(method.expectedClass.isSameOrSuperClassOf(this)) { "The method doesn't accept this class" }

        instanceMethods[name] = method
    }

    fun defineStaticFunction(function: SkFunction, name: String = function.name) {
        check(!staticFunctions.containsKey(name)) { "This class already has a static function named $name" }

        staticFunctions[name] = function
    }

    fun findInstanceMethod(key: String): SkMethod? {
        return instanceMethods[key] ?: superClass?.findInstanceMethod(key)
    }
}

fun SkClass.isSuperClassOf(clazz: SkClass): Boolean {
    var mama = clazz.superClass
    while (mama != null) {
        if (mama == this)
            return true
        mama = mama.superClass
    }
    return false
}

fun SkClass.isSameOrSuperClassOf(clazz: SkClass): Boolean {
    return clazz == this || isSuperClassOf(clazz)
}


object ObjectClass : SkClass("Object", null) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        TODO()
    }
}

object ClassClass : SkClass("Class", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        TODO()
    }
}

object FunctionClass : SkClass("Function", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        TODO()
    }
}

object NumberClass : SkClass("Number", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val valArg = kwArgs["value"] ?: posArgs.getOrNull(0) ?: SkNumber.ZERO
        return SkNumberObject(valArg.asNumber())
    }
}

object BooleanClass : SkClass("Boolean", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val valArg = kwArgs["value"] ?: posArgs.getOrNull(0) ?: SkBoolean.FALSE
        return SkBooleanObject(valArg.asBoolean())
    }
}

object StringClass : SkClass("String", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val valArg = kwArgs["value"] ?: posArgs.getOrNull(0) ?: SkString.EMPTY
        return SkStringObject(valArg.asString())
    }
}

object MapClass : SkClass("Map", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        TODO()
    }
}

object MethodClass : SkClass("Method", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        TODO()
    }
}