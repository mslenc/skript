package skript.values

import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.typeError
import skript.util.SkArguments

class SkMap : SkObject {
    override val klass: SkClassDef
        get() = SkMapClassDef

    constructor() : super()

    constructor(initialValues: Map<String, SkValue>) : this() {
        entries.putAll(initialValues)
    }

    override suspend fun propertySet(key: String, value: SkValue, env: SkriptEnv) {
        klass.findInstanceProperty(key)?.let { prop ->
            if (prop.readOnly)
                typeError("Can't set property $key, because it is read-only")

            prop.setValue(this, value, env)
            return
        }

        klass.findInstanceMethod(key)?.let {
            typeError("Can't override methods")
        }

        entrySet(key.toSkript(), value, env)
    }

    override suspend fun propertyGet(key: String, env: SkriptEnv): SkValue {
        klass.findInstanceProperty(key)?.let { prop ->
            return prop.getValue(this, env)
        }

        klass.findInstanceMethod(key)?.let { method ->
            return BoundMethod(method, this, SkArguments())
        }

        return entryGet(key.toSkript(), env)
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.MAP
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(entries.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        typeError("Can't convert a map into a number")
    }

    override fun asString(): SkString {
        return SkString.MAP
    }

    fun spreadFrom(values: SkMap) {
        if (values == this)
            return // ???

        entries.putAll(values.entries)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkMapIterator(this)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other is SkMap -> entries == other.entries
            else -> false
        }
    }

    override fun hashCode(): Int {
        return entries.hashCode()
    }
}

object SkMapClassDef : SkClassDef("Map") {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val result = SkMap()

        for (el in args.extractAllPosArgs()) {
            if (el is SkMap) {
                result.entries.putAll(el.entries)
            } else {
                typeError("Map constructor only accepts other Maps as positional arguments")
            }
        }

        result.entries.putAll(args.extractAllKwArgs())

        return result
    }
}