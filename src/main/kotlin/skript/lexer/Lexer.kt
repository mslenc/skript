package skript.lexer

import skript.lexer.TokenType.*
import skript.syntaxError
import java.lang.IllegalStateException

val isWhitespace: (Char)->Boolean = Character::isWhitespace
val notNewLine: (Char)->Boolean = { it != '\r' && it != '\n' }

private fun ArrayList<Token>.add(type: TokenType, rawText: String, pos: Pos) {
    // merging !in and !is into one token
    if (this.lastOrNull()?.type == EXCL) {
        if (type == IN || type == IS) {
            val prevIndex = this.size - 1
            val prev = this.last()

            val repl = if (type == IN) {
                Token(NOT_IN, "!in", prev.pos)
            } else {
                Token(NOT_IS, "!is", prev.pos)
            }

            set(prevIndex, repl)
            return
        }
    }

    add(Token(type, rawText, pos))
}

fun CharStream.lex(withEof: Boolean = true): List<Token> {
    val tokens = ArrayList<Token>()
    val sb = StringBuilder()

    nextToken@
    while (moreChars()) {
        if (skipWhile(isWhitespace) > 0)
            continue@nextToken

        if (consume('#')) {
            when {
                consume('*') -> consumeStarComment()
                consume('+') -> consumePlusComment()
                else -> skipWhile(notNewLine)
            }
            continue@nextToken
        }

        val pos = getPos()

        when (val c = nextChar()) {
            '(' -> { tokens.add(LPAREN, "(", pos) }
            ')' -> { tokens.add(RPAREN, ")", pos) }
            '{' -> { tokens.add(LCURLY, "{", pos) }
            '}' -> { tokens.add(RCURLY, "}", pos) }
            '[' -> { tokens.add(LBRACK, "[", pos) }
            ']' -> { tokens.add(RBRACK, "]", pos) }

            '@' -> { tokens.add(AT, "@", pos) }
            ',' -> { tokens.add(COMMA, ",", pos) }
            ':' -> { tokens.add(COLON, ":", pos) }
            ';' -> { tokens.add(SEMI, ";", pos) }

            '.' -> {
                when {
                    consume('.') -> when {
                        consume('<') -> tokens.add(DOT_DOT_LESS, "..<", pos)
                        else -> tokens.add(DOT_DOT, "..", pos)
                    }
                    else -> tokens.add(DOT, ".", pos)
                }
            }

            '!' -> {
                when {
                    consume('=') -> when {
                        consume('=') -> tokens.add(NOT_STRICT_EQUAL, "!==", pos)
                        else -> tokens.add(NOT_EQUAL, "!=", pos)
                    }
                    else -> tokens.add(EXCL, "!", pos)
                }
            }

            '+' -> when {
                consume('+') -> tokens.add(PLUS_PLUS, "++", pos)
                consume('=') -> tokens.add(PLUS_ASSIGN, "+=", pos)
                else -> tokens.add(PLUS, "+", pos)
            }

            '-' -> when {
                consume('-') -> tokens.add(MINUS_MINUS, "--", pos)
                consume('=') -> tokens.add(MINUS_ASSIGN, "-=", pos)
                consume('>') -> tokens.add(ARROW, "->", pos)
                else -> tokens.add(MINUS, "-", pos)
            }

            '*' -> when {
                consume('*') -> when {
                    consume('=') -> tokens.add(STAR_STAR_ASSIGN, "**=", pos)
                    else -> tokens.add(STAR_STAR, "**", pos)
                }
                consume('=') -> tokens.add(STAR_ASSIGN, "*=", pos)
                else -> tokens.add(STAR, "*", pos)
            }

            '/' -> when {
                consume('/') -> when {
                    consume('=') -> tokens.add(SLASH_SLASH_ASSIGN, "//=", pos)
                    else -> tokens.add(SLASH_SLASH, "//", pos)
                }
                consume('=') -> tokens.add(SLASH_ASSIGN, "/=", pos)
                else -> tokens.add(SLASH, "/", pos)
            }

            '%' -> when {
                consume('=') -> tokens.add(PERCENT_ASSIGN, "%=", pos)
                else -> tokens.add(PERCENT, "%", pos)
            }

            '&' -> when {
                consume('&') -> when {
                    consume('=') -> tokens.add(AND_AND_ASSIGN, "&&=", pos)
                    else -> tokens.add(AND_AND, "&&", pos)
                }
                consume('=') -> tokens.add(AND_ASSIGN, "&=", pos)
                else -> tokens.add(AND, "&", pos)
            }

            '|' -> when {
                consume('|') -> when {
                    consume('=') -> tokens.add(OR_OR_ASSIGN, "||=", pos)
                    else -> tokens.add(OR_OR, "||", pos)
                }
                consume('=') -> tokens.add(OR_ASSIGN, "|=", pos)
                else -> tokens.add(OR, "|", pos)
            }

            '=' -> when {
                consume('=') -> when {
                    consume('=') -> tokens.add(STRICT_EQUALS, "===", pos)
                    else -> tokens.add(EQUALS, "==", pos)
                }
                else -> tokens.add(ASSIGN, "=", pos)
            }

            '<' -> when {
                consume('=') -> when {
                    consume('>') -> tokens.add(STARSHIP, "<=>", pos)
                    else -> tokens.add(LESS_OR_EQUAL, "<=", pos)
                }
                else -> tokens.add(LESS_THAN, "<", pos)
            }

            '>' -> when {
                consume('=') -> tokens.add(GREATER_OR_EQUAL, ">=", pos)
                else -> tokens.add(GREATER_THAN, ">", pos)
            }

            '?' -> when {
                consume(':') -> tokens.add(ELVIS, "?:", pos)
                consume('.') -> tokens.add(SAFE_DOT, "?.", pos)
                else -> tokens.add(QUESTION, "?", pos)
            }

            else -> {
                // all we have left are IDENTIFIER, NUMBER, STRING, and keywords (as subset of IDENTIFIER)

                if (Character.isJavaIdentifierStart(c)) {
                    parseIdentOrKw(sb, c, tokens, pos)
                } else
                if (c in '0'..'9') {
                    parseNumber(sb, c, tokens, pos)
                } else
                if (c == '"' || c == '\'') {
                    parseString(sb, c, tokens, pos)
                } else {
                    // TODO: """ strings
                    // TODO: `...${}...` strings
                    syntaxError("Unexpected character ${c.toPrintable()}", pos)
                }
            }
        }
    }

    if (withEof) {
        tokens += Token(EOF, "", getPos())
    }

    return tokens
}

fun Char.toPrintable(): String {
    return when (Character.getType(this).toByte()) {
        Character.CURRENCY_SYMBOL,
        Character.DECIMAL_DIGIT_NUMBER,
        Character.LETTER_NUMBER,
        Character.LOWERCASE_LETTER,
        Character.TITLECASE_LETTER,
        Character.UPPERCASE_LETTER -> {
            this.toString()
        }

        else -> {
            "U+" + this.toInt().toString(16).padStart(4, '0')
        }
    }
}

val isNotQuoteOrBackSlash: (Char)->Boolean = { it != '"' && it != '\'' && it != '\\' }
private fun CharStream.parseString(sb: StringBuilder, quote: Char, tokens: ArrayList<Token>, pos: Pos) {
    sb.clear().append(quote)
    val otherQuote = if (quote == '"') '\'' else '"'

    while (moreChars()) {
        consumeInto(sb, isNotQuoteOrBackSlash)

        if (eof())
            syntaxError("Unterminated string", getPos())

        if (consume(quote)) {
            sb.append(quote)
            tokens.add(STRING, sb.toString(), pos)
            return
        }

        if (consume(otherQuote)) {
            sb.append(otherQuote)
            continue
        }

        sb.append(nextChar()) // must be backslash
        if (moreChars()) {
            sb.append(nextChar())
        } else {
            syntaxError("Unterminated string", getPos())
        }
    }
}

val isDigit: (Char)->Boolean = { it in '0'..'9' }
private fun CharStream.parseNumber(sb: StringBuilder, firstChar: Char, tokens: ArrayList<Token>, pos: Pos) {
    consumeInto(sb.clear().append(firstChar), isDigit)

    if (peek() == '.') {
        sb.append(nextChar())
        if (consumeInto(sb, isDigit) < 1) {
            sb.setLength(sb.length - 1)
            tokens.add(NUMBER, sb.toString(), pos)
            putBack('.')
            return
        }
    }

    if (peek() == 'e' || peek() == 'E') {
        sb.append(nextChar())
        if (peek() == '+' || peek() == '-') {
            sb.append(nextChar())
        }
        if (consumeInto(sb, isDigit) < 1) {
            syntaxError("Expected at least one digit in exponent", pos)
        }
    }

    tokens.add(NUMBER, sb.toString(), pos)
}

val isIdentChar: (Char)->Boolean = Character::isJavaIdentifierPart
private fun CharStream.parseIdentOrKw(sb: StringBuilder, firstChar: Char, tokens: ArrayList<Token>, pos: Pos) {
    consumeInto(sb.clear().append(firstChar), isIdentChar)
    val ident = sb.toString()

    val kwType = skriptKeywords[ident]
    if (kwType != null) {
        val str = internedKeywords[kwType] ?: throw IllegalStateException()
        tokens.add(kwType, str, pos)
    } else {
        tokens.add(IDENTIFIER, ident, pos)
    }
}


val notStar: (Char)->Boolean = { it != '*' }
private fun CharStream.consumeStarComment() {
    while (moreChars()) {
        skipWhile(notStar)
        if (consume('*') && consume('#'))
            return
    }
}

val notPlusNotHash: (Char)->Boolean = { it != '+' && it != '#' }
private fun CharStream.consumePlusComment() {
    var depth = 1
    while (moreChars()) {
        skipWhile(notPlusNotHash)
        if (consume('+') && consume('#')) {
            if (--depth < 1)
                return
        } else
        if (consume('#') && consume('+')) {
            depth++
        }
    }
}