package skript.lexer

import skript.syntaxError

class Tokens(tokens: List<Token>) {
    fun peek(): Token {
        TODO()
    }

    fun next(): Token {
        TODO()
    }

    fun consume(type: TokenType): Token? {
        TODO()
    }

    fun expect(type: TokenType): Token {
        if (peek().type == type) {
            return next()
        } else {
            syntaxError("Expected $type", peek().pos)
        }
    }
}