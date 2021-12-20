package skript.interop

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.values.SkObject

interface HoldsNative<T: Any> {
    val nativeObj: T
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

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.pojoNode(nativeObj)
    }
}

