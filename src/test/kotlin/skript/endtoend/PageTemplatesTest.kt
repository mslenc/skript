package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.runTemplate
import skript.values.SkList
import java.time.LocalDate
import java.time.LocalDateTime

class PageTemplatesTest {
    @Test
    fun pageTemplateBasics() = runBlocking {
        val result = runTemplate({ env ->
            env.setNativeGlobal("strings", SkList(listOf("a".toSkript(), "b<>d".toSkript(), "c''".toSkript())))
            env.setNativeGlobal("date", env.createNativeWrapper(LocalDateTime.of(2024, 9, 5, 12, 43, 44)))
        },
        """
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
        """, "html")

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
}