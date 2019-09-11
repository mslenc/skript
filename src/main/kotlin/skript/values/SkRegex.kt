package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.util.SkArguments
import skript.util.expectBoolean
import skript.util.expectString
import java.util.*

class SkRegex(val regex: Regex) : SkObject() {
    override val klass: SkClassDef
        get() = SkRegexClassDef


}

object SkRegex_containsMatchIn : SkMethod("containsMatchIn", listOf("input")) {
    override val expectedClass: SkClassDef
        get() = SkRegexClassDef

    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        val regex = (thiz as SkRegex).regex

        val input = args.expectString("input")
        args.expectNothingElse()

        return regex.containsMatchIn(input).toSkript()
    }
}



private inline fun SkArguments.ifTrue(key: String, block: ()->Unit) {
    if (expectBoolean(key, ifUndefined = false)) {
        block()
    }
}

object SkRegexClassDef : SkClassDef("Regex") {
    init {
        defineInstanceMethod(SkRegex_containsMatchIn)
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