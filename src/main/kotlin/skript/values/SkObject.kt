package skript.values

import skript.exec.RuntimeState
import skript.io.toSkript
import skript.notSupported
import skript.typeError
import skript.util.SkArguments

abstract class SkObject : SkValue() {
    internal val elements = LinkedHashMap<String, SkValue>()
    abstract val klass: SkClassDef

    override fun getKind(): SkValueKind {
        return SkValueKind.OBJECT
    }

    override fun asObject(): SkObject {
        return this
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.TRUE
    }

    override fun asNumber(): SkNumber {
        throw UnsupportedOperationException("Objects can't be converted into numbers")
    }

    override fun asString(): SkString {
        return SkString("[object ${klass.name}]")
    }

    override suspend fun elementSet(key: SkValue, value: SkValue, state: RuntimeState) {
        elements[key.asString().value] = value
    }

    override suspend fun elementGet(key: SkValue, state: RuntimeState): SkValue {
        return elements[key.asString().value] ?: SkUndefined
    }

    override suspend fun elementDelete(key: SkValue, state: RuntimeState): Boolean {
        return elements.remove(key.asString().value) != null
    }

    override suspend fun propGet(key: String, state: RuntimeState): SkValue {
        klass.findInstanceProperty(key)?.let { prop ->
            return prop.getValue(this, state.env)
        }

        klass.findInstanceMethod(key)?.let { method ->
            return BoundMethod(method, this, SkArguments())
        }

        return SkUndefined
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

        typeError("Can't create properties on objects; to set extra info on the object, use [\"${ key }\"] instead")
    }

    override suspend fun contains(key: SkValue, state: RuntimeState): Boolean {
        val keyStr = key.asString().value

        return elements.containsKey(keyStr)
    }

    override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        notSupported("Can't call objects")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        return callMethod("rangeTo", SkArguments.of(end, endInclusive.toSkript()), state, exprDebug)
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue {
        klass.findInstanceMethod(methodName)?.let { method ->
            return method.call(this, args, state)
        }

        throw UnsupportedOperationException("$exprDebug has no method $methodName")
    }
}

object SkObjectClassDef : SkClassDef("Object", null)