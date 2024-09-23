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

    fun putBack(token: Token) {
        check(pos > 0)
        check(tokens[--pos] === token)
    }
}

private val Token.isEcho
    get() = when (type) {
        TokenType.ECHO_WS,
        TokenType.ECHO_TEXT,
        TokenType.ECHO_NL -> true
        else -> false
    }

private fun ArrayList<Token>.append(token: Token) {
    if (token.isEcho) {
        val prev = lastOrNull()
        if (prev != null && prev.isEcho) {
            val combined = prev.rawText + token.rawText
            val repl = Token(TokenType.ECHO_TEXT, combined, prev.pos, combined)
            this[lastIndex] = repl
            return
        }
    }

    add(token)
}

private class RawTextMerger {
    private val out = ArrayList<Token>()
    private val text = ArrayList<Token>()

    fun dump() {
        val first = text.firstOrNull() ?: return
        val combinedText = text.joinToString("") { it.rawText }
        out += Token(TokenType.ECHO_TEXT, combinedText, first.pos, combinedText)
        text.clear()
    }

    fun append(token: Token) {
        if (token.isEcho) {
            text += token
        } else {
            dump()
            out += token
        }
    }

    fun result(): List<Token> {
        dump()
        return out
    }
}

fun List<Token>.cleanUpStmtOnlyLines(): List<Token> {
    var pos = 0
    var len = this.size
    val out = RawTextMerger()

    while (pos < len) {
        var first = pos
        var last = pos
        while (get(last).type != TokenType.ECHO_NL && last + 1 < len)
            last++

        var hasOutput = false
        var hasStmt = false
        for (i in first..last) {
            when (get(i).type) {
                TokenType.ECHO_TEXT -> hasOutput = true
                TokenType.STMT_OPEN -> hasStmt = true
                TokenType.EXPR_OPEN -> hasOutput = true
                else -> Unit
            }
        }

        if (hasStmt && !hasOutput) { // strip WS
            for (i in first..last) {
                val tok = get(i)
                when (tok.type) {
                    TokenType.ECHO_WS -> Unit
                    TokenType.ECHO_NL -> Unit
                    else -> out.append(tok)
                }
            }
        } else {
            for (i in first..last) {
                out.append(get(i))
            }
        }

        pos = last + 1
    }

    return out.result()
}