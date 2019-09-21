package skript.values

import skript.exec.RuntimeState
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
        elements.putAll(initialValues)
    }

    override suspend fun propSet(key: String, value: SkValue, state: RuntimeState) {
        klass.findInstanceProperty(key)?.let { prop ->
            if (prop.readOnly)
                typeError("Can't set property $key, because it is read-only")

            prop.setValue(this, value, state.env)
            return
        }

        klass.findInstanceMethod(key)?.let {
            typeError("Can't override methods")
        }

        elementSet(key.toSkript(), value, state)
    }

    override suspend fun propGet(key: String, state: RuntimeState): SkValue {
        klass.findInstanceProperty(key)?.let { prop ->
            return prop.getValue(this, state.env)
        }

        klass.findInstanceMethod(key)?.let { method ->
            return BoundMethod(method, this, SkArguments())
        }

        return elementGet(key.toSkript(), state)
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.MAP
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(elements.isNotEmpty())
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

        elements.putAll(values.elements)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkMapIterator(this)
    }
}

object SkMapClassDef : SkClassDef("Map") {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val result = SkMap()

        for (el in args.extractAllPosArgs()) {
            if (el is SkMap) {
                result.elements.putAll(el.elements)
            } else {
                typeError("Map constructor only accepts other Maps as positional arguments")
            }
        }

        result.elements.putAll(args.extractAllKwArgs())

        return result
    }
}