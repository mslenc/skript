package skript.values

import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.typeError
import skript.util.SkArguments

class SkMap() : SkAbstractMap(), MutableMap<String, SkValue> {
    override val klass: SkClassDef
        get() = SkMapClassDef

    constructor(initialValues: Map<String, SkValue>) : this() {
        entryMap.putAll(initialValues)
    }

    override val size: Int
        get() = entryMap.size

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

    override fun asString(): SkString {
        return SkString.MAP
    }

    fun spreadFrom(values: SkMap) {
        if (values == this)
            return // ???

        entryMap.putAll(values.entryMap)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkMapIterator(this)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other is SkMap -> entryMap == other.entryMap
            else -> false
        }
    }

    override fun hashCode(): Int {
        return entryMap.hashCode()
    }

    override fun unwrap(): Map<String, Any?> {
        return entryMap.mapValues { it.value.unwrap() }
    }

    override fun put(key: String, value: SkValue): SkValue? {
        return entryMap.put(key, value)
    }

    override fun get(key: String): SkValue? {
        return entryMap[key]
    }

    override fun remove(key: String): SkValue? {
        return entryMap.remove(key)
    }

    override fun containsKey(key: String): Boolean {
        return entryMap.containsKey(key)
    }

    override fun putAll(from: Map<out String, SkValue>) {
        return entryMap.putAll(from)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, SkValue>>
        get() = entryMap.entries

    override fun clear() {
        entryMap.clear()
    }

    override val keys: MutableSet<String>
        get() = entryMap.keys

    override val values: MutableCollection<SkValue>
        get() = entryMap.values

    override fun isEmpty(): Boolean {
        return entryMap.isEmpty()
    }

    override fun containsValue(value: SkValue): Boolean {
        return entryMap.containsValue(value)
    }
}

object SkMapClassDef : SkClassDef("Map", SkAbstractMapClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val result = SkMap()

        for (el in args.extractAllPosArgs()) {
            if (el is SkMap) {
                result.entryMap.putAll(el.entryMap)
            } else {
                typeError("Map constructor only accepts other Maps as positional arguments")
            }
        }

        result.entryMap.putAll(args.extractAllKwArgs())

        return result
    }
}