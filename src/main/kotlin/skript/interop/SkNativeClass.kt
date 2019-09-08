package skript.interop

import skript.exec.RuntimeState
import skript.typeError
import skript.values.ObjectClass
import skript.values.SkClass
import skript.values.SkValue

class SkNativeClass<T>(name: String, superClass: SkNativeClass<*>?) : SkClass(name, superClass ?: ObjectClass) {
    var constructor : SkNativeConstructor<T>? = null
    var properties = HashMap<String, SkNativeProperty<T, *>>()

    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        constructor?.let { constructor ->
            return constructor.call(posArgs, kwArgs, state)
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