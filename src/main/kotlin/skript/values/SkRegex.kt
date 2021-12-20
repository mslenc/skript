package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.interop.HoldsNative
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.util.SkArguments
import skript.util.expectBoolean
import skript.util.expectString
import java.util.*

class SkRegex(override val nativeObj: Regex) : SkObject(), HoldsNative<Regex> {
    override val klass: SkClassDef
        get() = SkRegexClassDef

    override fun unwrap(): Regex {
        return nativeObj
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.textNode(nativeObj.pattern)
    }
}

fun MatchGroup.toSkript() = SkMap(mapOf(
    "value" to this.value.toSkript(),
    "range" to this.range.toSkript()
))

fun IntRange.toSkript() = SkNumberRange(this.first.toSkript(), this.last.toSkript(), true)

private inline fun SkArguments.ifTrue(key: String, block: ()->Unit) {
    if (expectBoolean(key, ifUndefined = false)) {
        block()
    }
}

object SkRegexClassDef : SkCustomClass<SkRegex>("Regex") {
    init {
        defineMethod("containsMatchIn").
            withStringParam("input").
            withImpl { regex, input, _ ->
                regex.nativeObj.containsMatchIn(input).toSkript()
            }

        defineMethod("find").
            withStringParam("input").
            withIntParam("startIndex", defaultValue = 0).
            withImpl { regex, input, startIndex, _ ->
                val match = regex.nativeObj.find(input, startIndex) ?: return@withImpl SkNull
                SkMatchResult(match)
            }

        defineMethod("findAll").
            withStringParam("input").
            withIntParam("startIndex", defaultValue = 0).
            withImpl { regex, input, startIndex, _ ->
                val matches = regex.nativeObj.findAll(input, startIndex)
                SkList(matches.map { SkMatchResult(it) }.toList())
            }

        defineMethod("matchEntire").
            withStringParam("input").
            withImpl { regex, input, _ ->
                val match = regex.nativeObj.matchEntire(input) ?: return@withImpl SkNull
                SkMatchResult(match)
            }

        defineMethod("matches", isInfix = true).
            withStringParam("input").
            withImpl { regex, input, _ ->
                regex.nativeObj.matches(input).toSkript()
            }

        defineMethod("replace").
            withStringParam("input").
            withParam("replacement").
            withImpl { regex, input, repl, state ->
                if (repl !is SkFunction)
                    return@withImpl regex.nativeObj.replace(input, repl.asString().value).toSkript()

                var match = regex.nativeObj.find(input) ?: return@withImpl input.toSkript()
                val inputLen = input.length
                val sb = StringBuilder(inputLen)
                var pos = 0

                while (pos < inputLen) {
                    val range = match.range
                    if (range.first > pos)
                        sb.append(input, pos, range.start)

                    val replaced = repl.call(SkArguments.of(SkMatchResult(match)), state)
                    sb.append(replaced.asString().value)
                    pos = range.last + 1

                    match = match.next() ?: break
                }

                if (pos < inputLen)
                    sb.append(input, pos, inputLen)

                sb.toString().toSkript()
            }

        defineMethod("replaceFirst").
            withStringParam("input").
            withStringParam("replacement").
            withImpl { regex, input, replacement, _ ->
                regex.nativeObj.replaceFirst(input, replacement).toSkript()
            }

        defineMethod("split").
            withStringParam("input").
            withIntParam("limit", defaultValue = 0).
            withImpl { regex, input, limit, _ ->
                val results = regex.nativeObj.split(input, limit)
                SkList(results.map { it.toSkript() })
            }

        defineStaticFunction(object : SkFunction("escape", listOf("literal")) {
            override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
                val literal = args.expectString("literal")
                args.expectNothingElse()

                return Regex.escape(literal).toSkript()
            }
        })

        defineStaticFunction(object : SkFunction("escapeReplacement", listOf("literal")) {
            override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
                val literal = args.expectString("literal")
                args.expectNothingElse()

                return Regex.escapeReplacement(literal).toSkript()
            }
        })

        defineStaticFunction(object : SkFunction("fromLiteral", listOf("literal")) {
            override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
                val literal = args.expectString("literal")
                args.expectNothingElse()

                return SkRegex(Regex.fromLiteral(literal))
            }
        })
    }

    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val pattern = args.expectString("pattern")

        val options = EnumSet.noneOf(RegexOption::class.java)
        args.ifTrue("ignoreCase") { options += RegexOption.IGNORE_CASE }
        args.ifTrue("multiline") { options += RegexOption.MULTILINE }
        args.ifTrue("literal") { options += RegexOption.LITERAL }
        args.ifTrue("unixLines") { options += RegexOption.UNIX_LINES }
        args.ifTrue("comments") { options += RegexOption.COMMENTS }
        args.ifTrue("dotMatchesAll") { options += RegexOption.DOT_MATCHES_ALL }
        args.ifTrue("canonEq") { options += RegexOption.CANON_EQ }

        return SkRegex(Regex(pattern, options))
    }
}

class SkMatchGroup(override val nativeObj: MatchGroup): SkObject(), HoldsNative<MatchGroup> {
    override val klass: SkClassDef
        get() = SkMatchGroupClassDef

    override fun unwrap(): MatchGroup {
        return nativeObj
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.textNode(nativeObj.value)
    }
}

object SkMatchGroupClassDef : SkCustomClass<SkMatchGroup>("MatchGroup") {
    init {
        defineReadOnlyProperty("value") { it.nativeObj.value.toSkript() }
        defineReadOnlyProperty("range") { it.nativeObj.range.toSkript() }
    }
}

class SkMatchResult(override val nativeObj: MatchResult) : SkObject(), HoldsNative<MatchResult> {
    override val klass: SkClassDef
        get() = SkMatchResultClassDef

    override fun unwrap(): MatchResult {
        return nativeObj
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.textNode(nativeObj.value)
    }
}

object SkMatchResultClassDef : SkCustomClass<SkMatchResult>("MatchResult") {
    init {
        defineReadOnlyProperty("value") { it.nativeObj.value.toSkript() }
        defineReadOnlyProperty("range") { it.nativeObj.range.toSkript() }

        defineMethod("next").withImpl {
            when (val nextResult = it.nativeObj.next()) {
                null -> SkNull
                else -> SkMatchResult(nextResult)
            }
        }
    }
}