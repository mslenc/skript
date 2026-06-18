package skript.interop

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.values.SkMap
import skript.values.SkObject
import skript.values.SkString
import skript.values.SkValue

interface HoldsNative<T: Any> {
    val nativeObj: T
}

interface JsonSettings {
    val factory: JsonNodeFactory

    /**
     * Allows the library user to override serialization method for particular types.
     * If null is returned, the standard serialization will be done.
     * In particular, for most native objects, this means calling factory.pojoNode().
     **/
    suspend fun customSerialize(obj: Any): JsonNode? {
        return null
    }

    /**
     * Allows the library user to do the reverse of customSerialize.
     * If nothing custom is to be done, null should be returned (in which case the value will deserialize to a SkMap)
     */
    suspend fun customDeserialize(props: Map<String, SkValue>, env: SkriptEnv): SkValue? {
        return null
    }

    companion object {
        val DEFAULT_FACTORY: JsonNodeFactory = JsonNodeFactory(true)

        val DEFAULT = object : JsonSettings {
            override val factory: JsonNodeFactory
                get() = DEFAULT_FACTORY
        }
    }
}

class SkNativeObject<T: Any>(override val nativeObj: T, override val klass: SkNativeClassDef<T>) : SkObject(), HoldsNative<T> {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is SkNativeObject<*> -> nativeObj == other.nativeObj
            else -> false
        }
    }

    override fun hashCode(): Int {
        return nativeObj.hashCode()
    }

    override fun unwrap(): Any {
        return nativeObj
    }

    override fun asString(): SkString {
        return nativeObj.toString().toSkript()
    }

    override suspend fun toJson(settings: JsonSettings): JsonNode {
        return settings.customSerialize(nativeObj) ?: settings.factory.pojoNode(nativeObj)
    }
}

