package skript.values

import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments

open class SkClassDef(val name: String, val superClass: SkClassDef? = null) {
    internal val instanceMethods = HashMap<String, SkMethod>()
    internal val staticFunctions = HashMap<String, SkFunction>()

    open suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        typeError("Can't construct new instances of $name")
    }

    open fun checkNameAvailable(name: String) {
        check(!instanceMethods.containsKey(name)) { "This class already has a method named $name" }
    }

    open fun defineInstanceMethod(method: SkMethod, name: String = method.name) {
        checkNameAvailable(name)
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

    fun isInstance(value: SkValue): Boolean {
        if (value !is SkObject)
            return false

        var klass = value.klass
        while (klass != this)
            klass = klass.superClass ?: return false

        return true
    }
}