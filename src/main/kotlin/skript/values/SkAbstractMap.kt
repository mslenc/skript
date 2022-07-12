package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.typeError

abstract class SkAbstractMap : SkObject() {
    abstract fun getSize(): Int

    override suspend fun contains(key: SkValue, env: SkriptEnv): Boolean {
        return entryGet(key, env) != SkUndefined
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(getSize() > 0)
    }

    override fun asNumber(): SkNumber {
        typeError("Can't convert a map into a number")
    }

    override fun asString(): SkString {
        return SkString.MAP
    }

    abstract override suspend fun makeIterator(): SkIterator

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        val obj = factory.objectNode()
        val it = makeIterator()

        while (it.moveToNext()) {
            val key = it.getCurrentKey().asString().value
            val value = it.getCurrentValue().toJson(factory)

            obj.set<JsonNode>(key, value)
        }

        return obj
    }
}

object SkAbstractMapClassDef : SkCustomClass<SkAbstractMap>("AbstractMap", SkObjectClassDef) {
    init {
        defineReadOnlyProperty("size",
            getter = { it.getSize().toSkript() }
        )

        defineMethod("put").
            withParam("key").
            withParam("value").
            withImpl { map, key, value, env ->
                map.entrySet(key, value, env)
                SkUndefined
            }

        defineMethod("keys").withImpl { map ->
            val keys = SkList()
            map.makeIterator().let {
                while (it.moveToNext()) {
                    keys.add(it.getCurrentKey())
                }
            }
            keys
        }

        defineMethod("remove").
            withParam("key").
            withImpl { map, key, env ->
                map.entryDelete(key, env).toSkript()
            }
    }
}
