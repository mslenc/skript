package skript.interop

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.JsonNodeType
import skript.values.SkAbstractList
import skript.values.SkBoolean
import skript.values.SkClass
import skript.values.SkDecimal
import skript.values.SkDouble
import skript.values.SkFunction
import skript.values.SkList
import skript.values.SkMap
import skript.values.SkMethod
import skript.values.SkNull
import skript.values.SkObject
import skript.values.SkString
import skript.values.SkUndefined
import skript.values.SkValue
import skript.values.SkValueKind
import java.math.BigDecimal

interface JsonCodec<T> {
    suspend fun toJson(value: SkValue): T
    suspend fun fromJson(json: T): SkValue

    suspend fun toJsonString(value: SkValue): String
    suspend fun fromJsonString(json: String): SkValue

    companion object {
        val DEFAULT_MAPPER = ObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
        val DEFAULT = JacksonJsonBuilder(DEFAULT_MAPPER)
    }
}

abstract class AbstractJsonCodec<T> : JsonCodec<T> {
    protected val containerStack = ArrayList<SkValue>()
    protected val currentPath = ArrayList<Any>()

    abstract fun finalizeMap(values: Map<String, T>): T
    abstract fun finalizeList(elements: List<T>): T

    abstract fun convertNull(): T
    abstract fun convertUndefined(): T
    abstract fun convertNumber(number: Double): T
    abstract fun convertDecimal(number: BigDecimal): T
    abstract fun convertBoolean(value: Boolean): T
    abstract fun convertString(value: String): T

    open suspend fun convertClass(value: SkClass): T {
        throw IllegalArgumentException("Can't serialize classes.")
    }

    open suspend fun convertObject(value: SkObject): T {
        throw IllegalArgumentException("Can't serialize objects.")
    }

    open suspend fun convertFunction(value: SkFunction): T {
        throw IllegalArgumentException("Can't serialize functions.")
    }

    open suspend fun convertMethod(value: SkMethod): T {
        throw IllegalArgumentException("Can't serialize methods.")
    }

    override suspend fun toJson(value: SkValue): T {
        return when (value.getKind()) {
            SkValueKind.NULL -> convertNull()
            SkValueKind.UNDEFINED -> convertUndefined()
            SkValueKind.NUMBER -> convertNumber(value.asNumber().toDouble())
            SkValueKind.DECIMAL -> convertDecimal(value.asNumber().toBigDecimal())
            SkValueKind.BOOLEAN -> convertBoolean(value.asBoolean().value)
            SkValueKind.STRING -> convertString(value.asString().value)

            SkValueKind.LIST -> {
                val list = value as SkAbstractList

                if (containerStack.any { it === list })
                    throw IllegalStateException("Can't serialize cyclic structures.")
                containerStack.add(list)
                try {
                    val elements = ArrayList<T>()
                    for (i in 0..<list.getSize()) {
                        val skEl = list.getSlot(i)
                        currentPath.add(i)
                        try {
                            elements += toJson(skEl)
                        } finally {
                            currentPath.removeLast()
                        }
                    }
                    finalizeList(elements)
                } finally {
                    containerStack.removeLast()
                }
            }

            SkValueKind.MAP -> {
                val map = value as SkMap

                if (containerStack.any { it === map })
                    throw IllegalStateException("Can't serialize cyclic structures.")
                containerStack.add(map)
                try {
                    val values = LinkedHashMap<String, T>()
                    map.entries.forEach { (key, value) ->
                        currentPath.add(key)
                        try {
                            values[key] = toJson(value)
                        } finally {
                            currentPath.removeLast()
                        }
                    }
                    finalizeMap(values)
                } finally {
                    containerStack.removeLast()
                }
            }

            SkValueKind.CLASS -> convertClass(value as SkClass)
            SkValueKind.OBJECT -> convertObject(value as SkObject)
            SkValueKind.FUNCTION -> convertFunction(value as SkFunction)
            SkValueKind.METHOD -> convertMethod(value as SkMethod)
        }
    }
}

open class JacksonJsonBuilder(val mapper: ObjectMapper) : AbstractJsonCodec<JsonNode>() {
    override fun finalizeMap(values: Map<String, JsonNode>): JsonNode {
        return mapper.createObjectNode().also {
            it.setAll<JsonNode>(values)
        }
    }

    override fun finalizeList(elements: List<JsonNode>): JsonNode {
        return mapper.createArrayNode().also {
            it.addAll(elements)
        }
    }

    override fun convertNull(): JsonNode {
        return mapper.nullNode()
    }

    override fun convertUndefined(): JsonNode {
        return mapper.missingNode()
    }

    override fun convertNumber(number: Double): JsonNode {
        return mapper.nodeFactory.numberNode(number)
    }

    override fun convertDecimal(number: BigDecimal): JsonNode {
        return mapper.nodeFactory.numberNode(number)
    }

    override fun convertBoolean(value: Boolean): JsonNode {
        return mapper.nodeFactory.booleanNode(value)
    }

    override fun convertString(value: String): JsonNode {
        return mapper.nodeFactory.textNode(value)
    }

    override suspend fun toJsonString(value: SkValue): String {
        return mapper.writeValueAsString(toJson(value))
    }

    override suspend fun fromJsonString(json: String): SkValue {
        return fromJson(mapper.readTree(json))
    }

    protected open suspend fun overrideDeserialize(fields: Map<String, SkValue>): SkValue? {
        return null
    }

    override suspend fun fromJson(json: JsonNode): SkValue {
        return when (json.nodeType) {
            JsonNodeType.NULL -> SkNull
            JsonNodeType.MISSING -> SkUndefined
            JsonNodeType.BOOLEAN -> SkBoolean.valueOf(json.asBoolean())
            JsonNodeType.STRING -> SkString(json.textValue())
            JsonNodeType.NUMBER -> when {
                json.isFloat -> SkDouble.valueOf(json.asDouble())
                json.isDouble -> SkDouble.valueOf(json.asDouble())
                else -> SkDecimal.valueOf(json.decimalValue())
            }

            JsonNodeType.ARRAY -> {
                val list = SkList()
                for (el in json.elements())
                    list.add(fromJson(el))
                list
            }

            JsonNodeType.OBJECT -> {
                val map = SkMap()
                for ((key, value) in json.properties()) {
                    map[key] = fromJson(value)
                }

                overrideDeserialize(map.entryMap) ?: map
            }

            JsonNodeType.BINARY,
            JsonNodeType.POJO ->
                throw IllegalArgumentException("Can't deserialize ${json.nodeType} from json.")
        }
    }
}