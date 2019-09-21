package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments

class SkClass(val def: SkClassDef, val superClass: SkClass?) : SkObject() {
    val name: String
        get() = def.className

    override val klass: SkClassDef
        get() = SkClassClassDef

    suspend fun construct(args: SkArguments, env: SkriptEnv): SkValue {
        return def.construct(this, args, env)
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.CLASS
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        return construct(args, state.env)
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue {
        def.findStaticFunction(methodName)?.let { function ->
            return function.call(args, state)
        }

        typeError("$exprDebug is class ${def.className}, which has no static function $methodName")
    }

    override suspend fun propGet(key: String, state: RuntimeState): SkValue {
        def.findStaticProperty(key)?.let { prop ->
            return prop.getValue(state.env)
        }

        typeError("Class ${def.className} has no static property $key")
    }

    override suspend fun propSet(key: String, value: SkValue, state: RuntimeState) {
        def.findStaticProperty(key)?.let { prop ->
            prop.setValue(value, state.env)
        }
    }

    fun isInstance(value: SkValue): Boolean {
        return def.isInstance(value)
    }
}

fun SkClassDef.isSuperClassOf(clazz: SkClassDef): Boolean {
    val me = this
    var mama = clazz.superClass
    while (mama != null) {
        if (mama == me)
            return true
        mama = mama.superClass
    }
    return false
}

fun SkClassDef.isSameOrSuperClassOf(clazz: SkClassDef): Boolean {
    return clazz == this || isSuperClassOf(clazz)
}

object SkClassClassDef : SkClassDef("Class")











