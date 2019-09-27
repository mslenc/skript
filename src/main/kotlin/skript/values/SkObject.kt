package skript.values

import skript.exec.RuntimeState
import skript.io.toSkript
import skript.typeError
import skript.util.SkArguments

abstract class SkObject : SkValue() {
    internal val entries = LinkedHashMap<String, SkValue>()
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
        typeError("Objects can't be converted into numbers")
    }

    override fun asString(): SkString {
        return SkString("[object ${klass.className}]")
    }

    override suspend fun entrySet(key: SkValue, value: SkValue, state: RuntimeState) {
        entries[key.asString().value] = value
    }

    override suspend fun entryGet(key: SkValue, state: RuntimeState): SkValue {
        return entries[key.asString().value] ?: SkUndefined
    }

    override suspend fun entryDelete(key: SkValue, state: RuntimeState): Boolean {
        return entries.remove(key.asString().value) != null
    }

    override suspend fun propertyGet(key: String, state: RuntimeState): SkValue {
        klass.findInstanceProperty(key)?.let { prop ->
            return prop.getValue(this, state.env)
        }

        klass.findInstanceMethod(key)?.let { method ->
            return BoundMethod(method, this, SkArguments())
        }

        return SkUndefined
    }

    override suspend fun propertySet(key: String, value: SkValue, state: RuntimeState) {
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

        return entries.containsKey(keyStr)
    }

    override suspend fun call(args: SkArguments, state: RuntimeState): SkValue {
        typeError("Can't call objects")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        return callMethod("rangeTo", SkArguments.of(end, endInclusive.toSkript()), state, exprDebug)
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, state: RuntimeState, exprDebug: String): SkValue {
        klass.findInstanceMethod(methodName)?.let { method ->
            return method.call(this, args, state)
        }

        typeError("$exprDebug has no method $methodName")
    }
}

object SkObjectClassDef : SkClassDef("Object", null) {
    init {
        defineInstanceMethod(object : SkMethod("toString", emptyList()) {
            override val expectedClass: SkClassDef get() = SkObjectClassDef
            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState) = thiz.asString()
        })

        defineInstanceMethod(object : SkMethod("toBoolean", emptyList()) {
            override val expectedClass: SkClassDef get() = SkObjectClassDef
            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState) = thiz.asBoolean()
        })

        defineInstanceMethod(object : SkMethod("toNumber", emptyList()) {
            override val expectedClass: SkClassDef get() = SkObjectClassDef
            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState) = thiz.asNumber()
        })
    }
}