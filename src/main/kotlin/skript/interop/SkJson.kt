package skript.interop

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.values.*

object SkJson {
    private val jsonFactory = JsonFactory()
    private val nodeFactory = JsonNodeFactory.withExactBigDecimals(true)

    fun parse(json: String): SkValue {
        val parser: JsonParser = jsonFactory.createParser(json)

        val top = BuilderTop()
        val stack = ArrayList<Builder>()
        stack += top


        nextToken@
        while (!parser.isClosed) {
            val token = parser.nextToken()
            if (token == null)
                break

            val value: SkValue

            when (token) {
                JsonToken.NOT_AVAILABLE -> throw IllegalStateException("Token not available during JSON parsing.")
                JsonToken.VALUE_EMBEDDED_OBJECT -> throw IllegalStateException("Embedded object encountered during JSON parsing.")


                JsonToken.START_OBJECT -> {
                    stack += BuilderMap()
                    continue@nextToken
                }

                JsonToken.END_OBJECT -> {
                    if (stack.size < 2) throw IllegalStateException("Object ended at top level.")
                    val objBuilder = stack.removeLast()
                    objBuilder as? BuilderMap ?: throw IllegalStateException("Object ended, but not building an object.")
                    stack.last().addValue(objBuilder.build())
                    continue@nextToken
                }


                JsonToken.START_ARRAY -> {
                    stack += BuilderList()
                    continue@nextToken
                }

                JsonToken.END_ARRAY -> {
                    if (stack.size < 2) throw IllegalStateException("Array ended at top level.")
                    val listBuilder = stack.removeLast()
                    listBuilder as? BuilderList ?: throw IllegalStateException("Array ended, but not building an array.")
                    stack.last().addValue(listBuilder.build())
                    continue@nextToken
                }


                JsonToken.FIELD_NAME -> {
                    stack.last().setFieldName(parser.currentName)
                    continue@nextToken
                }

                JsonToken.VALUE_STRING -> {
                    value = SkString(parser.valueAsString)
                }

                JsonToken.VALUE_NUMBER_INT -> {
                    value = SkDecimal.valueOf(parser.bigIntegerValue)
                }

                JsonToken.VALUE_NUMBER_FLOAT -> {
                    value = SkDecimal.valueOf(parser.decimalValue)
                }

                JsonToken.VALUE_TRUE,
                JsonToken.VALUE_FALSE -> {
                    value = if (parser.valueAsBoolean) SkBoolean.TRUE else SkBoolean.FALSE
                }

                JsonToken.VALUE_NULL -> {
                    value = SkNull
                }
            }

            stack.last().addValue(value)
        }

        if (stack.size != 1)
            throw IllegalArgumentException("Not JSON - illegal state at end of input.")

        return stack.last().build()
    }

    suspend fun stringify(value: SkValue): String {
        return value.toJson(nodeFactory).toPrettyString()
    }
}

private sealed class Builder {
    abstract fun setFieldName(name: String)
    abstract fun addValue(value: SkValue)
    abstract fun build(): SkValue
}

private class BuilderTop : Builder() {
    private var result: SkValue? = null

    override fun setFieldName(name: String) {
        throw IllegalStateException("Field name on top level.")
    }

    override fun addValue(value: SkValue) {
        result?.let { throw IllegalStateException("Multiple values on top level.") }
        result = value
    }

    override fun build(): SkValue {
        result?.let { return it }

        throw IllegalStateException("Missing value on top level.")
    }
}

private class BuilderList : Builder() {
    private val elements = ArrayList<SkValue>()

    override fun setFieldName(name: String) {
        throw IllegalStateException("Unexpected field name inside a list.")
    }

    override fun addValue(value: SkValue) {
        elements.add(value)
    }

    override fun build(): SkList {
        return SkList(elements)
    }
}

private class BuilderMap : Builder() {
    var key: String? = null
    val values = LinkedHashMap<String, SkValue>()

    override fun setFieldName(name: String) {
        key?.let { throw IllegalStateException("Unexpected double field name.") }
        key = name
    }

    override fun addValue(value: SkValue) {
        key?.let {
            val prev = values.put(it, value)
            if (prev != null)
                throw IllegalStateException("Field name used twice inside a map.")
            key = null
            return
        }

        throw IllegalStateException("Missing field name inside a map.")
    }

    override fun build(): SkValue {
        return SkMap(values)
    }
}