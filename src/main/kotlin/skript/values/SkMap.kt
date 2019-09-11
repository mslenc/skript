package skript.values

import skript.io.SkriptEnv
import skript.notSupported
import skript.typeError
import skript.util.SkArguments

class SkMap : SkObject {
    override val klass: SkClassDef
        get() = SkMapClassDef

    constructor() : super()

    constructor(initialValues: Map<String, SkValue>) : this() {
        for ((key, value) in initialValues) {
            defaultSetMember(key, value)
        }
    }

    internal fun setMemberInternal(key: SkValue, value: SkValue) {
        defaultSetMember(key.asString().value, value)
    }

    fun setMapMember(key: String, value: SkValue) {
        props = props.withAdded(key, value)
    }

    fun getMapMember(key: String): SkValue? {
        return props.get(key)
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.MAP
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(props != EmptyProps)
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert a map into a number")
    }

    override fun asString(): SkString {
        return SkString.MAP
    }

    fun spreadFrom(values: SkMap) {
        if (values == this)
            return // ???

        values.props.forEach { key, value ->
            defaultSetMember(key, value)
        }
    }

    override suspend fun makeIterator(): SkValue {
        return SkMapIterator(this)
    }
}

object SkMapClassDef : SkClassDef("Map", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val result = SkMap()
        for (el in args.getRemainingPosArgs()) {
            if (el is SkMap) {
                el.props.forEach { key, value ->
                    result.setMapMember(key, value)
                }
            } else {
                typeError("Map constructor only accepts other Maps as positional arguments")
            }
        }

        for ((key, value) in args.getRemainingKwArgs()) {
            result.setMapMember(key, value)
        }

        return result
    }
}