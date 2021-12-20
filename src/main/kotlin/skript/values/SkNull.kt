package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments

object SkNull : SkScalar() {
    override fun asObject(): SkObject {
        typeError("Can't convert null into an object")
    }

    override suspend fun propertySet(key: String, value: SkValue, env: SkriptEnv) {
        typeError("Can't set properties on null")
    }

    override suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv) {
        typeError("Can't set elements on null")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, env: SkriptEnv, exprDebug: String): SkValue {
        typeError("$exprDebug evaluated to null, so range can't be created")
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.NULL
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.FALSE
    }

    override fun asNumber(): SkNumber {
        return SkNumber.ZERO
    }

    override fun asString(): SkString {
        return SkString.NULL
    }

    override fun toString(sb: StringBuilder) {
        sb.append("null")
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, env: SkriptEnv, exprDebug: String): SkValue {
        typeError("$exprDebug is null, so can't call methods on it")
    }

    override fun unwrap(): Any? {
        return null
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.nullNode()
    }
}