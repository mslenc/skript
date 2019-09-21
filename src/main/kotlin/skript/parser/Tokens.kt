package skript.parser

import skript.syntaxError

class Tokens(val tokens: List<Token>) {
    var pos: Int = 0

    fun hasMore(): Boolean {
        return pos < tokens.size
    }

    fun peek(): Token {
        return tokens[pos]
    }

    fun peekOnNewLine(): Boolean {
        val pos = pos
        return pos > 0 && tokens[pos].pos.row > tokens[pos - 1].pos.row
    }

    fun next(): Token {
        return tokens[pos++]
    }

    fun consume(type: TokenType): Token? {
        val token = tokens[pos]
        return if (token.type == type) {
            pos++
            token
        } else {
            null
        }
    }

    fun expect(type: TokenType): Token {
        if (peek().type == type) {
            return next()
        } else {
            syntaxError("Expected $type", peek().pos)
        }
    }

    fun atEndOfStatement(): Boolean {
        return when (peek().type) {
            TokenType.SEMI,
            TokenType.RCURLY,
            TokenType.EOF -> true
            else -> peekOnNewLine()
        }
    }

    fun expectEndOfStatement() {
        if (consume(TokenType.SEMI) == null && !atEndOfStatement()) {
            syntaxError("Expected end of statement (a semi-colon ; or a curly brace } or a new line)", peek().pos)
        }
    }

    fun putBack(token: Token) {
        check(pos > 0)
        check(tokens[--pos] === token)
    }
}