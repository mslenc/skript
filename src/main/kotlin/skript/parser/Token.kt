package skript.lexer

data class Pos(val row: Int, val col: Int, val file: String)

data class Token(val type: TokenType, val rawText: String, val pos: Pos)