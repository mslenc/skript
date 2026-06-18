package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.interop.JsonSettings

class SkStringBuilder : SkObject() {
    val sb = StringBuilder()

    override val klass: SkClassDef
        get() = SkStringBuilderClassDef

    fun appendRawText(text: String) {
        sb.append(text)
    }

    fun append(value: SkValue) {
        sb.append(value.asString().value)
    }

    override fun asString(): SkString {
        return SkString(sb.toString())
    }

    override fun unwrap(): StringBuilder {
        return sb
    }

    override suspend fun toJson(settings: JsonSettings): JsonNode {
        return settings.factory.textNode(sb.toString())
    }
}

object SkStringBuilderClassDef : SkClassDef("StringBuilder")