package skript.io

import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class ModuleSource {
    abstract val moduleName: ModuleName

    abstract fun prepare(engine: SkriptEngine): PreparedModule
}

inline fun <reified T> getKTypeOf(value: T): KType = typeOf<T>()

fun main() {
    val map = LinkedHashMap<String, Number>()
    val type = getKTypeOf(map)
    println(type)
}