package skript.interop

import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments
import skript.values.*
import kotlin.reflect.KClass

class SkNativeClassDef<T : Any>(name: String, val nativeClass: KClass<T>, superClass: SkNativeClassDef<*>?) : SkClassDef(name, superClass ?: SkObjectClassDef) {
    var constructor : SkNativeConstructor<T>? = null
    var properties = HashMap<String, SkNativeProperty<T, *>>()

    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        constructor?.let { constructor ->
            return constructor.call(args, env)
        }

        typeError("Can't construct instances of $name")
    }

    override fun checkNameAvailable(name: String) {
        super.checkNameAvailable(name)
        check(!properties.containsKey(name)) { "This class already has $name defined" }
    }

    fun defineProperty(property: SkNativeProperty<T, *>) {
        checkNameAvailable(property.name)

        properties[property.name] = property
    }
}