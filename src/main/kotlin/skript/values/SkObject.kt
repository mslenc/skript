package skript.values

import skript.io.SkriptEnv
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

    override suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv) {
        entries[key.asString().value] = value
    }

    override suspend fun entryGet(key: SkValue, env: SkriptEnv): SkValue {
        return entries[key.asString().value] ?: SkUndefined
    }

    override suspend fun entryDelete(key: SkValue, env: SkriptEnv): Boolean {
        return entries.remove(key.asString().value) != null
    }

    override suspend fun propertyGet(key: String, env: SkriptEnv): SkValue {
        klass.findInstanceProperty(key)?.let { prop ->
            return prop.getValue(this, env)
        }

        klass.findInstanceMethod(key)?.let { method ->
            return BoundMethod(method, this, SkArguments())
        }

        return SkUndefined
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

        typeError("Can't create properties on objects; to set extra info on the object, use [\"${ key }\"] instead")
    }

    override suspend fun contains(key: SkValue, env: SkriptEnv): Boolean {
        val keyStr = key.asString().value

        return entries.containsKey(keyStr)
    }

    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        typeError("Can't call objects")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, env: SkriptEnv, exprDebug: String): SkValue {
        return callMethod("rangeTo", SkArguments.of(end, endInclusive.toSkript()), env, exprDebug)
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, env: SkriptEnv, exprDebug: String): SkValue {
        klass.findInstanceMethod(methodName)?.let { method ->
            return method.call(this, args, env)
        }

        typeError("$exprDebug has no method $methodName")
    }

    override fun unwrap(): Any {
        return this
    }
}

object SkObjectClassDef : SkClassDef("Object", null) {
    init {
        defineInstanceMethod(object : SkMethod("toString", emptyList()) {
            override val expectedClass: SkClassDef get() = SkObjectClassDef
            override suspend fun call(thiz: SkValue, args: SkArguments, env: SkriptEnv) = thiz.asString()
        })

        defineInstanceMethod(object : SkMethod("toBoolean", emptyList()) {
            override val expectedClass: SkClassDef get() = SkObjectClassDef
            override suspend fun call(thiz: SkValue, args: SkArguments, env: SkriptEnv) = thiz.asBoolean()
        })

        defineInstanceMethod(object : SkMethod("toNumber", emptyList()) {
            override val expectedClass: SkClassDef get() = SkObjectClassDef
            override suspend fun call(thiz: SkValue, args: SkArguments, env: SkriptEnv) = thiz.asNumber()
        })
    }
}