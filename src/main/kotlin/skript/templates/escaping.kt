package skript.templates

import skript.io.SkriptEnv
import skript.io.toSkript
import skript.util.SkArguments
import skript.values.SkFunction
import skript.values.SkString
import java.util.*

object EscapeRaw : SkFunction("raw", listOf("value")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkString {
        return args.extractArg("value").asString()
    }
}

object EscapeHtml : SkFunction("html", listOf("value")) {
    private val encode = BitSetBuilder().add("&<>\"'").build()

    override suspend fun call(args: SkArguments, env: SkriptEnv): SkString {
        val value = args.extractArg("value")
        val raw = value.asString().value
        return escape(raw).toSkript()
    }

    fun escape(raw: String): String {
        val startAt = raw.indexOfFirst { encode.get(it.code) }
        if (startAt < 0)
            return raw

        val sb = StringBuilder()
        sb.append(raw, 0, startAt)
        for (i in startAt..<raw.length) {
            when (val c = raw[i]) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(c)
            }
        }

        return sb.toString()
    }
}

object EscapeJs : SkFunction("js", listOf("value")) {
    private val encode = BitSetBuilder().add("\'\"\\\n\r\t\b\u000B\u000C\u0000").build()

    override suspend fun call(args: SkArguments, env: SkriptEnv): SkString {
        val value = args.extractArg("value")
        val raw = value.asString().value
        return escape(raw).toSkript()
    }

    fun escape(raw: String): String {
        val startAt = raw.indexOfFirst { encode.get(it.code) }
        if (startAt < 0)
            return raw

        val sb = StringBuilder()
        sb.append(raw, 0, startAt)
        for (i in startAt..<raw.length) {
            when (val c = raw[i]) {
                '\'' -> sb.append("\\'")
                '\"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000B' -> sb.append("\\v")
                '\u000C' -> sb.append("\\f")
                '\u0000' -> sb.append("\\u0000")

                else -> sb.append(c)
            }
        }

        return sb.toString()
    }
}

object EscapeUrl : SkFunction("url", listOf("value")) {
    private val dontEncode = BitSetBuilder().addRange('a', 'z').addRange('A', 'Z').addRange('0', '9').add("-_.!~*'(),;:").build()

    override suspend fun call(args: SkArguments, env: SkriptEnv): SkString {
        val value = args.extractArg("value")
        val raw = value.asString().value
        return escape(raw).toSkript()
    }

    fun escape(raw: String): String {
        if (raw.all { dontEncode.get(it.code) })
            return raw

        val sb = StringBuilder()

        for (b in raw.encodeToByteArray()) {
            val i = b.toUByte().toInt()

            if (dontEncode.get(i)) {
                sb.append(i.toChar())
            } else {
                sb.append('%')
                sb.append("0123456789ABCDEF"[i / 16])
                sb.append("0123456789ABCDEF"[i % 16])
            }
        }

        return sb.toString()
    }
}

object EscapeMarkdown : SkFunction("markdown", listOf("value")) {
    private val encode = BitSetBuilder().add("\\`*_{}[]<>()#+-.!|").build()

    override suspend fun call(args: SkArguments, env: SkriptEnv): SkString {
        val value = args.extractArg("value")
        val raw = value.asString().value
        return escape(raw).toSkript()
    }

    fun escape(raw: String): String {
        val startAt = raw.indexOfFirst { encode.get(it.code) }
        if (startAt < 0)
            return raw

        val sb = StringBuilder(raw.length + 16)

        sb.append(raw, 0, startAt)
        for (i in startAt..<raw.length) {
            val c = raw[i]

            if (encode.get(c.code))
                sb.append('\\')

            sb.append(c)
        }

        return sb.toString()
    }
}

private class BitSetBuilder {
    val result = BitSet()

    fun add(c: Char): BitSetBuilder {
        result.set(c.code)
        return this
    }

    fun addRange(from: Char, to: Char): BitSetBuilder {
        for (c in from.code .. to.code)
            result.set(c)
        return this
    }

    fun add(chars: String): BitSetBuilder {
        for (c in chars)
            add(c)
        return this
    }

    fun build(): BitSet {
        return result
    }
}