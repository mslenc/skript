package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.io.SkriptEnv
import skript.typeError
import skript.util.SkArguments

object SkUndefined : SkScalar() {
    override fun asObject(): SkObject {
        typeError("Can't convert undefined into an object")
    }

    override suspend fun propertySet(key: String, value: SkValue, env: SkriptEnv) {
        typeError("Can't set properties on undefined")
    }

    override suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv) {
        typeError("Can't set elements on undefined")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, env: SkriptEnv, exprDebug: String): SkValue {
        typeError("$exprDebug evaluated to undefined, so range can't be created")
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.UNDEFINED
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.FALSE
    }

    override fun asNumber(): SkNumber {
        typeError("Can't convert undefined into a number")
    }

    override fun asString(): SkString {
        return SkString.UNDEFINED
    }

    override fun toString(sb: StringBuilder) {
        sb.append("undefined")
    }

    override suspend fun callMethod(methodName: String, args: SkArguments, env: SkriptEnv, exprDebug: String): SkValue {
        typeError("$exprDebug is undefined, so can't call methods on it")
    }

    override fun unwrap(): Any? {
        return null
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.missingNode()
    }
}