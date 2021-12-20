package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory

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

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.textNode(sb.toString())
    }
}

object SkStringBuilderClassDef : SkClassDef("StringBuilder")