package skript.endtoend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import skript.asStringOrNull
import skript.assertEmittedEquals
import skript.interop.JsonSettings
import skript.interop.SkJson
import skript.io.SkriptEnv
import skript.io.pack
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.templates.nullIfNull
import skript.values.SkMap
import skript.values.SkString
import skript.values.SkValue
import skript.values.SkValueKind

class MapLiteralTest {
    @Test
    fun testMapLiteralBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            val fifth = "fiverr"
            
            val first = { a: "A", b: "B", "c:d": 12, fifth };
            
            val third = "3!"
            val fourth = 123 - 119
            
            val second = {
                **first,
                third,
                [ first.a ]: "bigA",
                fourth,
            };

            for ((key, value) in first) {
                emit(key);
                emit(value);
            }
            
            for ((key, value) in second) {
                emit(key);
                emit(value);
            }

        """.trimIndent())

        val expect = listOf(
            "a".toSkript(), "A".toSkript(),
            "b".toSkript(), "B".toSkript(),
            "c:d".toSkript(), 12.toSkript(),
            "fifth".toSkript(), "fiverr".toSkript(),

            "a".toSkript(), "A".toSkript(),
            "b".toSkript(), "B".toSkript(),
            "c:d".toSkript(), 12.toSkript(),
            "fifth".toSkript(), "fiverr".toSkript(),
            "third".toSkript(), "3!".toSkript(),
            "A".toSkript(), "bigA".toSkript(),
            "fourth".toSkript(), 4.toSkript(),
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testConversionToJson() = runBlocking {
        val outputs = runScriptWithEmit("""
            
            emit({
                foo: "bar",
                list: [ 1, 2.43, 3.011d ],
                bools: {
                    t: true,
                    f: false
                }
            });
            
        """.trimIndent())

        val json = outputs[0].toJson()

        assertEquals("""
            {"foo":"bar","list":[1.0,2.43,3.011],"bools":{"t":true,"f":false}}
        """.trimIndent(), json.toString())
    }

    @Test
    fun testRemovingWorks() = runBlocking {
        val outputs = runScriptWithEmit("""
            val map1 = { a: "bcd", e: "fgh" }
            emit(map1)
            
            val map2 = { a: "bcd", e: "fgh" }
            map2["e"] = undefined
            emit(map2)
            
            val map3 = { a: "bcd", e: "fgh" }
            map3.remove("e")
            emit(map3)
            
        """.trimIndent())

        assertEquals("{s1as3bcds1es3fgh}", pack(outputs[0]))
        assertEquals("{s1as3bcds1eu}", pack(outputs[1]))
        assertEquals("{s1as3bcd}", pack(outputs[2]))
    }

    @Test
    fun testParsingJson() = runBlocking {
        val outputs = runScriptWithEmit(
            {
                it.setNativeGlobal("JSON", SkJson(JsonSettings.DEFAULT, it))
                it.setGlobal("jsonSource", """
                    {
                        "foo": "bar",
                        "list": [ 1, 2.43, 3.011 ],
                        "bools": {
                            "t": true,
                            "f": false,
                            "n": null
                        }
                    }
                """.trimIndent().toSkript())
            },

            """
            
            emit(JSON.parse(jsonSource))
            emit(JSON.stringify(JSON.parse(jsonSource)))
            
        """.trimIndent())

        val json = outputs[0].toJson()

        assertEquals("""
            {"foo":"bar","list":[1,2.43,3.011],"bools":{"t":true,"f":false,"n":null}}
        """.trimIndent(), json.toString())

        assertEquals("{s3foos3bars4list[d11d42.43d53.011]s5bools{s1tTs1fFs1nU}}", pack(outputs[0]))


        val json2 = outputs[1]

        assertEquals("""
            {
              "foo" : "bar",
              "list" : [ 1, 2.43, 3.011 ],
              "bools" : {
                "t" : true,
                "f" : false,
                "n" : null
              }
            }
        """.trimIndent(), json2.asString().value)
    }

    @Test
    fun testParsingCustomJson() = runBlocking {
        val outputs = runScriptWithEmit(
            {
                it.setNativeGlobal("JSON", SkJson(JsonTestCustomExporter(), it))
                it.setNativeGlobal("obj", JsonTestObject("obj123"))
                it.setNativeGlobal("ent", JsonTestEntity("ent234"))

                it.setGlobal("jsonSource", """
                    {
                        "abc": "def",
                        "objRef": { "_t": "Object", "id": "o345" },
                        "entRef": { "_t": "Entity", "id": "e456" },
                        "flag": true
                    }
                """.trimIndent().toSkript())
            },

            """
            
            emit(JSON.parse(jsonSource))
            emit(JSON.stringify({
                theObject: obj,
                theEntity: ent
            }))
            emit(JSON.parse(JSON.stringify({
                theObj: obj,
                theEnt: ent
            })))
            
        """.trimIndent())

        val res1 = outputs[0] as SkMap
        assertEquals(4, res1.getSize())
        assertEquals("def", res1.entries["abc"]?.unwrap())
        assertEquals(true, res1.entries["flag"]?.unwrap())
        assertEquals(JsonTestObject("o345"), res1.entries["objRef"]?.unwrap())
        assertEquals(JsonTestEntity("e456"), res1.entries["entRef"]?.unwrap())

        val res2 = outputs[1] as SkString
        assertEquals("""
        {
          "theObject" : {
            "_t" : "Object",
            "id" : "obj123"
          },
          "theEntity" : {
            "_t" : "Entity",
            "id" : "ent234"
          }
        }""".trimIndent(), res2.unwrap())

        val res3 = outputs[2] as SkMap
        assertEquals(2, res3.getSize())
        assertEquals(JsonTestObject("obj123"), res3.entries["theObj"]?.unwrap())
        assertEquals(JsonTestEntity("ent234"), res3.entries["theEnt"]?.unwrap())
    }
}

interface JsonTestExportable {
    fun toJson(factory: JsonNodeFactory): JsonNode
}

private fun JsonNodeFactory.refMap(t: String, id: String): ObjectNode {
    val map = this.objectNode()
    map.put("_t", t)
    map.put("id", id)
    return map
}

data class JsonTestEntity(val id: String) : JsonTestExportable {
    override fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.refMap("Entity", id)
    }
}

data class JsonTestObject(val id: String) : JsonTestExportable {
    override fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.refMap("Object", id)
    }
}

class JsonTestCustomExporter : JsonSettings {
    override val factory: JsonNodeFactory
        get() = JsonSettings.DEFAULT_FACTORY

    override suspend fun customSerialize(obj: Any): JsonNode? {
        if (obj is JsonTestExportable)
            return obj.toJson(factory)

        return null
    }

    override suspend fun customDeserialize(props: Map<String, SkValue>, env: SkriptEnv): SkValue? {
        val t = props["_t"]?.asStringOrNull() ?: return null
        val id = props["id"]?.asStringOrNull() ?: return null

        return when (t) {
            "Entity" -> env.createNativeWrapper(JsonTestEntity(id))
            "Object" -> env.createNativeWrapper(JsonTestObject(id))
            else -> null
        }
    }
}