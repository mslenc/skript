package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.util.SkArguments

class SkClass(val def: SkClassDef, val superClass: SkClass?) : SkObject() {
    val name: String
        get() = def.name

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

    fun findInstanceMethod(key: String): SkMethod? {
        return def.instanceMethods[key] ?: superClass?.findInstanceMethod(key)
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

object SkClassClassDef : SkClassDef("Class", SkObjectClassDef)











