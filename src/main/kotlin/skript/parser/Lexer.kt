package skript.parser

import skript.parser.TokenType.*
import skript.syntaxError
import java.lang.IllegalStateException

val isWhitespace: (Char)->Boolean = Character::isWhitespace
val notNewLine: (Char)->Boolean = { it != '\r' && it != '\n' }

private fun ArrayList<Token>.add(type: TokenType, rawText: String, pos: Pos, value: Any = rawText) {
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

    add(Token(type, rawText, pos, value))
}

sealed class TemplatePart
data class TemplatePartString(val text: String) : TemplatePart()
data class TemplatePartExpr(val tokens: List<Token>) : TemplatePart()

val notTickNotBackSlashNotDollar: (Char)->Boolean = { it != '`' && it != '\\' && it != '$' }
val isHexChar: (Char)->Boolean = { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

fun CharStream.lexTemplate(startPos: Int, pos: Pos): Token {
    val parts = ArrayList<TemplatePart>()
    val sb = StringBuilder()

    nextPart@
    while (moreChars()) {
        consumeInto(sb, notTickNotBackSlashNotDollar)

        if (consume('`')) {
            if (sb.isNotEmpty()) {
                parts.add(TemplatePartString(sb.toString()))
            }
            return Token(TEMPLATE, rawTextSince(startPos), pos, parts)
        }

        if (consume('$')) {
            if (consume('{')) {
                if (sb.isNotEmpty()) {
                    parts.add(TemplatePartString(sb.toString()))
                    sb.clear()
                }

                val tokens = lex(inTemplate = true)
                parts += TemplatePartExpr(tokens)
                continue@nextPart
            } else {
                sb.append('$')
                continue@nextPart
            }
        }

        if (consume('\\')) {
            doEscapeChar(sb)
        }
    }

    syntaxError("Unterminated string template", getPos())
}

fun CharStream.doEscapeChar(out: StringBuilder) {
    when (nextChar()) {
        'r' -> out.append('\r')
        'n' -> out.append('\n')
        't' -> out.append('\t')
        'b' -> out.append('\b')
        '"' -> out.append('"')
        '\'' -> out.append('\'')
        '`' -> out.append('`')
        '$' -> out.append('$')
        '\\' -> out.append('\\')
        'u' -> {
            val hexStr = nextChars(4, isHexChar) ?: syntaxError("Expected 4 hexadecimal characters after \\u", getPos())
            out.append(hexStr.toInt(16).toChar())
        }
        else -> {
            syntaxError("Unsupported escape sequence", getPos())
        }
    }
}

fun CharStream.lex(withEof: Boolean = true, inTemplate: Boolean = false): List<Token> {
    val tokens = ArrayList<Token>()
    val sb = StringBuilder()
    var curlyDepth = 1

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
        val startPos = currentPos()

        when (val c = nextChar()) {
            '{' -> {
                tokens.add(LCURLY, "{", pos)
                curlyDepth++
            }

            '}' -> {
                if (inTemplate && curlyDepth == 1) {
                    break@nextToken
                } else {
                    tokens.add(RCURLY, "}", pos)
                    curlyDepth--
                }
            }

            '(' -> { tokens.add(LPAREN, "(", pos) }
            ')' -> { tokens.add(RPAREN, ")", pos) }
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
                    parseString(sb, c, tokens, pos, startPos)
                } else
                if (c == '`') {
                    tokens.add(lexTemplate(startPos, pos))
                } else {
                    // TODO: """ strings
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
            "U+" + this.code.toString(16).padStart(4, '0')
        }
    }
}

val isNotQuoteOrBackSlash: (Char)->Boolean = { it != '"' && it != '\'' && it != '\\' }
private fun CharStream.parseString(sb: StringBuilder, quote: Char, tokens: ArrayList<Token>, pos: Pos, startPos: Int) {
    sb.clear()
    val otherQuote = if (quote == '"') '\'' else '"'

    while (moreChars()) {
        consumeInto(sb, isNotQuoteOrBackSlash)

        if (eof())
            syntaxError("Unterminated string", getPos())

        if (consume(quote)) {
            tokens.add(STRING, rawTextSince(startPos), pos, sb.toString())
            return
        }

        if (consume(otherQuote)) {
            sb.append(otherQuote)
            continue
        }

        // must be backslash
        nextChar()
        doEscapeChar(sb)
    }
}

val isDigit: (Char)->Boolean = { it in '0'..'9' }
private fun CharStream.parseNumber(sb: StringBuilder, firstChar: Char, tokens: ArrayList<Token>, pos: Pos) {
    sb.clear().append(firstChar)
    consumeInto(sb, isDigit)

    if (peek() == '.') {
        sb.append(nextChar())
        if (consumeInto(sb, isDigit) < 1) {
            putBack('.')
            sb.setLength(sb.length - 1)
            tokens.add(DOUBLE, sb.toString(), pos)
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

    if (peek() == 'd' || peek() == 'D') {
        val beforePrefix = sb.toString()
        sb.append(nextChar())
        tokens.add(DECIMAL, sb.toString(), pos, beforePrefix)
    } else {
        tokens.add(DOUBLE, sb.toString(), pos)
    }
}

val isIdentChar: (Char)->Boolean = Character::isJavaIdentifierPart
private fun CharStream.parseIdentOrKw(sb: StringBuilder, firstChar: Char, tokens: ArrayList<Token>, pos: Pos) {
    sb.clear().append(firstChar)
    consumeInto(sb, isIdentChar)
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