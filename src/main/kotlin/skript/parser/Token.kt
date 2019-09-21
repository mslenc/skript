package skript.parser

data class Pos(val row: Int, val col: Int, val file: String) {
    override fun toString(): String {
        return "$file:$row:$col"
    }
}

data class Token(val type: TokenType, val rawText: String, val pos: Pos, val value: Any = rawText)