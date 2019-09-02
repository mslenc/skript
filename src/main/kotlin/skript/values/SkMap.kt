package skript.values

import skript.notSupported

class SkMap : SkObject {
    constructor() : super(MapClass)

    constructor(initialValues: Map<String, SkValue>) : this() {
        for ((key, value) in initialValues) {
            defaultSetMember(key, value)
        }
    }

    internal fun setMemberInternal(key: SkValue, value: SkValue) {
        defaultSetMember(key, value)
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
}