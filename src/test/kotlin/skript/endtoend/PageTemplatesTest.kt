package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.ModuleProvider
import skript.io.ModuleSourceProvider
import skript.io.SkriptEngine
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.runTemplate
import skript.templates.TemplateRuntime
import skript.values.SkList
import skript.values.SkMap
import java.time.LocalDate
import java.time.LocalDateTime

class PageTemplatesTest {
    @Test
    fun pageTemplateBasics() = runBlocking {
        val ctx = mapOf(
            "strings" to listOf("a", "b<>d", "c''"),
            "date" to LocalDateTime.of(2024, 9, 5, 12, 43, 44),
        )

        val result = runTemplate(ctx = ctx, escape = "html", template = """
            {% val num1 = 12 %}
            {% var num2 = 25 %}
            Hello!
            {% if num1 > num2 %}
            Num1 is bigger ({{ num1 }} > {{ num2 }}).
            {% elif num2 > num1 %}
            Num2 is bigger ({{ num2 }} > {{ num1 }}).
            {% else %}
            The nums are the same ({{ num1 }} = {{ num2 }}).
            {% end if %}
            
            {% num2 //= 3 %}
            Num2 is now {{ num2 }}.
            
            {{ date }}
            {{ date |> date }}
            {{ date |> date("E a") }}
            {{ date |> time("long") }}
            {{ date |> dateTime("full") -> url }}

            {% for i, str in strings %}
            {{ i }}: {{ str }} / "{{ str -> js }}" / "{{ str -> js -> html }}"
            {% end %}
        """)

        val expect = """
            Hello!
            Num2 is bigger (25 > 12).
            
            Num2 is now 8.
            
            2024-09-05T12:43:44
            9/5/24
            Thu PM
            12:43:44 PM PDT
            Thursday,%20September%205,%202024%2012:43:44%20PM%20PDT

            0: a / "a" / "a"
            1: b&lt;&gt;d / "b<>d" / "b&lt;&gt;d"
            2: c&#39;&#39; / "c\'\'" / "c\&#39;\&#39;"
        """

        assertEquals(expect, result)
    }

    @Test
    fun blocksBasics() = runBlocking {
        val templates = mapOf(
            "base" to """
                <html>
                    <head>
                        {% block head %}{{ title }}{% end %}
                    </head>
                    <body>
                        {% block body %}
                            {% block header %}{% end %}
                            {% block main %}{% end %}
                            {% block footer %}{% end %}
                        {% end %}
                    </body>
                </html>
            """.trimIndent(),

            "default" to """
                {% extends "base" %}
                {% block header %}
                    {% include "navbar" with { **ctx } %}
                {% end block %}
                {% block footer %}
                    {% include block header with { navTitle: "footer" }%}
                {% end block footer %}
            """.trimIndent(),

            "navbar" to "        <nav>{{ navTitle }}</nav>\n",

            "home" to """
                {% extends "default" %}
                {% block head %}<title>Skript.io - {% include super block with { title: "Home" } %}</title>{% end %}
                {% block body %}
                        <div>Top of body</div>
                    {% include super block with { navTitle: "home" }%}
                        <div>Bottom of body</div>
                {% end %}
                {% block header %}
                    {% if navTitle == "footer" %}
                        {% include super block %}
                    {% else %}
                        {% include super block with { navTitle: "newHeader" } %}
                    {% end if %}
                {% end block %}
            """.trimIndent()
        )

        val engine = SkriptEngine()
        val env = engine.createEnv(moduleProvider = ModuleProvider.from(ModuleSourceProvider.static(emptyMap(), templates)))

        suspend fun runTemplate(templateName: String): String {
            val template = env.loadTemplate(templateName)
            val sb = StringBuilder()
            val out = TemplateRuntime.createWithDefaults(sb)
            val ctx = SkMap(mapOf(
                "title" to "The Title".toSkript(),
                "navTitle" to "The NavTitle".toSkript(),
            ))
            template.execute(ctx, out)
            return sb.toString()
        }

        val expectBase = """
            <html>
                <head>
                    The Title
                </head>
                <body>
                </body>
            </html>
        """.trimIndent()

        val expectDefault = """
            <html>
                <head>
                    The Title
                </head>
                <body>
                    <nav>The NavTitle</nav>
                    <nav>footer</nav>
                </body>
            </html>
        """.trimIndent()

        val expectHome = """
            <html>
                <head>
                    <title>Skript.io - Home</title>
                </head>
                <body>
                    <div>Top of body</div>
                    <nav>newHeader</nav>
                    <nav>footer</nav>
                    <div>Bottom of body</div>
                </body>
            </html>

        """.trimIndent()

        assertEquals(expectBase, runTemplate("base"))
        assertEquals(expectDefault, runTemplate("default"))
        assertEquals(expectHome, runTemplate("home"))
    }
}