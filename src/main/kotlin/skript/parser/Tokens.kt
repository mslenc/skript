package skript.lexer

import skript.syntaxError

class Tokens(val tokens: List<Token>) {
    var pos: Int = 0

    fun peek(): Token {
        return tokens[pos]
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
}