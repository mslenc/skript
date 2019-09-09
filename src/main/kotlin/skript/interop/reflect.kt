package skript.interop

import skript.io.SkriptEngine
import kotlin.reflect.KClass

fun <T: Any> reflectNativeClass(klass: KClass<T>, superClass: SkNativeClassDef<*>, engine: SkriptEngine): SkNativeClassDef<T> {
    TODO()
}