package skript.values

class SkStringBuilder() : SkObject() {
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
}

object SkStringBuilderClassDef : SkClassDef("StringBuilder", null)