package skript.interop

import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments
import skript.values.*
import kotlin.reflect.KClass

class SkNativeClassDef<T : Any>(name: String, val nativeClass: KClass<T>, superClass: SkNativeClassDef<*>?) : SkClassDef(name, superClass ?: SkObjectClassDef) {
    var constructor : SkNativeConstructor<T>? = null

    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        constructor?.let { constructor ->
            return constructor.call(args, env)
        }

        typeError("Can't construct instances of $name")
    }

    fun defineNativeProperty(property: SkNativeProperty<T, *>) {
        defineInstanceProperty(object : SkObjectProperty() {
            override val expectedClass: SkClassDef
                get() = this@SkNativeClassDef

            override val name: String
                get() = property.name

            override val nullable: Boolean
                get() = property.nullable

            override val readOnly: Boolean
                get() = property.readOnly

            private fun getNativeObj(obj: SkObject): T {
                obj as? SkNativeObject<*> ?: typeError("Accessing property $name on wrong class object")
                val nativeObj = obj.nativeObj
                @Suppress("UNCHECKED_CAST")
                if (nativeClass.isInstance(nativeObj)) {
                    return nativeObj as T
                } else {
                    typeError("Accessing property $name on wrong class object")
                }
            }

            override suspend fun getValue(obj: SkObject, env: SkriptEnv): SkValue {
                return property.getValue(getNativeObj(obj), env)
            }

            override suspend fun setValue(obj: SkObject, value: SkValue, env: SkriptEnv) {
                property.setValue(getNativeObj(obj), value, env)
            }
        })
    }
}