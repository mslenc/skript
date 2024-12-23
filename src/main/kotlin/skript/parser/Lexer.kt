package skript.parser

import skript.parser.TokenType.*
import skript.syntaxError
import java.lang.IllegalStateException
import java.lang.ProcessBuilder.Redirect.PIPE

val isWhitespace: (Char)->Boolean = Character::isWhitespace
val notNewLine: (Char)->Boolean = { it != '\r' && it != '\n' }
val notTickNotBackSlashNotDollar: (Char)->Boolean = { it != '`' && it != '\\' && it != '$' }
val isHexChar: (Char)->Boolean = { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
val notLeftCurlyOrNL: (Char)->Boolean = { it != '{' && it != '\r' && it != '\n' }

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
            return Token(STRING_TEMPLATE, rawTextSince(startPos), pos, parts)
        }

        if (consume('$')) {
            if (consume('{')) {
                if (sb.isNotEmpty()) {
                    parts.add(TemplatePartString(sb.toString()))
                    sb.clear()
                }

                val tokens = lexStringTemplateExpr()
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



fun CharStream.consumeComment(allowSingleLine: Boolean): Boolean {
    if (consume('#')) {
        when {
            consume('*') -> { consumeStarComment(); return true }
            consume('+') -> { consumePlusComment(); return true }
            else -> {
                if (allowSingleLine) {
                    skipWhile(notNewLine)
                    return true
                } else {
                    syntaxError("Unexpected # - single line comments not allowed here.", getPos(-1))
                }
            }
        }
    }
    return false
}

fun CharStream.lexPageTemplate(): List<Token> {
    val tokens = ArrayList<Token>()
    val sb = StringBuilder()

    outside@
    while (moreChars()) {
        val pos = getPos()

        sb.setLength(0)
        echoText@
        while (true) {
            consumeInto(sb, notLeftCurlyOrNL)
            // skip solo {
            if (consume('{')) {
                val next = peek()
                if (next != '{' && next != '#' && next != '%') {
                    sb.append('{')
                    continue@echoText
                } else {
                    putBack('{')
                }
            }

            if (sb.length > 0) {
                val s = sb.toString()
                val hasText = s.any { it != ' ' && it != '\t' }
                val type = if (hasText) ECHO_TEXT else ECHO_WS
                tokens += Token(type, s, pos, s)
                continue@outside
            } else {
                break
            }
        }

        if (consume('\r')) {
            if (consume('\n')) {
                tokens += Token(ECHO_NL, "\r\n", pos, "\r\n")
            } else {
                tokens += Token(ECHO_NL, "\r", pos, "\r")
            }
            continue@outside
        }

        if (consume('\n')) {
            tokens += Token(ECHO_NL, "\n", pos, "\n")
            continue@outside
        }

        if (consume("{#")) {
            consumeTemplateComment()
            continue@outside
        }

        if (consume("{{")) {
            tokens += Token(EXPR_OPEN, "{{", pos)
            var curlyDepth = 0

            expr@
            while (moreChars()) {
                if (skipWhile(isWhitespace) > 0)
                    continue@expr

                if (consumeComment(false))
                    continue@expr

                if (curlyDepth == 0 && consume("}}")) {
                    tokens += Token(EXPR_CLOSE, "}}", getPos(-2))
                    break@expr
                }

                val token = lexRegularToken()
                when (token.type) {
                    LCURLY, LPAREN, LBRACK -> { curlyDepth++ }
                    RCURLY, RPAREN, RBRACK -> { curlyDepth--; if (curlyDepth < 0) syntaxError("Unexpected " + token.rawText, token.pos) }
                    else -> Unit
                }
                tokens += token
            }

            continue@outside
        }

        if (consume("{%")) {
            tokens += Token(STMT_OPEN, "{%", pos)
            var curlyDepth = 0

            stmt@
            while (moreChars()) {
                if (skipWhile(isWhitespace) > 0)
                    continue@stmt

                if (consumeComment(false))
                    continue@stmt

                if (curlyDepth == 0 && consume("%}")) {
                    tokens += Token(STMT_CLOSE, "%}", getPos(-2))
                    break@stmt
                }

                val token = lexRegularToken()
                when (token.type) {
                    LCURLY, LPAREN, LBRACK -> { curlyDepth++ }
                    RCURLY, RPAREN, RBRACK -> { curlyDepth--; if (curlyDepth < 0) syntaxError("Unexpected " + token.rawText, token.pos) }
                    else -> Unit
                }
                tokens += token
            }

            continue@outside
        }
    }

    return tokens.withEof(this)
}

fun ArrayList<Token>.withEof(cs: CharStream): List<Token> {
    add(Token(EOF, "", cs.getPos()))
    return this
}

fun CharStream.lexCodeModule(): List<Token> {
    val tokens = ArrayList<Token>()

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

        tokens += lexRegularToken()
    }

    return tokens.withEof(this)
}

fun CharStream.lexStringTemplateExpr(): List<Token> {
    var curlyDepth = 1
    val tokens = ArrayList<Token>()

    nextToken@
    while (moreChars()) {
        if (skipWhile(isWhitespace) > 0)
            continue@nextToken

        if (peek() == '#') {
            val pos = getPos()
            consume('#')

            when {
                consume('*') -> consumeStarComment()
                consume('+') -> consumePlusComment()
                else -> syntaxError("Unexpected # inside a placeholder", pos)
            }
            continue@nextToken
        }

        val token = lexRegularToken()
        when (token.type) {
            LCURLY -> {
                tokens += token
                curlyDepth++
            }

            RCURLY -> {
                if (curlyDepth == 1)
                    break@nextToken

                curlyDepth--
                tokens += token
            }

            else -> {
                tokens += token
            }
        }
    }

    return tokens.withEof(this)
}

fun CharStream.lexRegularToken(): Token {
    val pos = getPos()
    val startPos = currentPos()

    when (val c = nextChar()) {
        '{' -> return Token(LCURLY, "{", pos)
        '}' -> return Token(RCURLY, "}", pos)

        '(' -> return Token(LPAREN, "(", pos)
        ')' -> return Token(RPAREN, ")", pos)
        '[' -> return Token(LBRACK, "[", pos)
        ']' -> return Token(RBRACK, "]", pos)

        '@' -> return Token(AT, "@", pos)
        ',' -> return Token(COMMA, ",", pos)
        ':' -> return Token(COLON, ":", pos)
        ';' -> return Token(SEMI, ";", pos)

        '.' -> return when {
            consume(".<") -> Token(DOT_DOT_LESS, "..<", pos)
            consume('.') -> Token(DOT_DOT, "..", pos)
            else -> return Token(DOT, ".", pos)
        }

        '!' -> return when {
            consume("==") -> Token(NOT_STRICT_EQUAL, "!==", pos)
            consume("=") -> Token(NOT_EQUAL, "!=", pos)
            consumeNotId("in") -> Token(NOT_IN, "!in", pos)
            consumeNotId("is") -> Token(NOT_IS, "!is", pos)
            else -> Token(EXCL, "!", pos)
        }

        '+' -> return when {
            consume('+') -> Token(PLUS_PLUS, "++", pos)
            consume('=') -> Token(PLUS_ASSIGN, "+=", pos)
            else -> Token(PLUS, "+", pos)
        }

        '-' -> return when {
            consume('-') -> Token(MINUS_MINUS, "--", pos)
            consume('=') -> Token(MINUS_ASSIGN, "-=", pos)
            consume('>') -> Token(ARROW, "->", pos)
            else -> Token(MINUS, "-", pos)
        }

        '*' -> return when {
            consume("*=") -> Token(STAR_STAR_ASSIGN, "**=", pos)
            consume('*') -> Token(STAR_STAR, "**", pos)
            consume('=') -> Token(STAR_ASSIGN, "*=", pos)
            else -> Token(STAR, "*", pos)
        }

        '/' -> return when {
            consume("/=") -> Token(SLASH_SLASH_ASSIGN, "//=", pos)
            consume('/') -> Token(SLASH_SLASH, "//", pos)
            consume('=') -> Token(SLASH_ASSIGN, "/=", pos)
            else -> Token(SLASH, "/", pos)
        }

        '%' -> return when {
            consume('=') -> Token(PERCENT_ASSIGN, "%=", pos)
            else -> Token(PERCENT, "%", pos)
        }

        '&' -> return when {
            consume("&=") -> Token(AND_AND_ASSIGN, "&&=", pos)
            consume('&') -> Token(AND_AND, "&&", pos)
            consume('=') -> Token(AND_ASSIGN, "&=", pos)
            else -> Token(AND, "&", pos)
        }

        '|' -> return when {
            consume("|=") -> Token(OR_OR_ASSIGN, "||=", pos)
            consume('|') -> Token(OR_OR, "||", pos)
            consume('>') -> Token(PIPE_CALL, "|>", pos)
            consume('=') -> Token(OR_ASSIGN, "|=", pos)
            else -> Token(OR, "|", pos)
        }

        '=' -> return when {
            consume("==") -> Token(STRICT_EQUALS, "===", pos)
            consume('=') -> Token(EQUALS, "==", pos)
            else -> Token(ASSIGN, "=", pos)
        }

        '<' -> return when {
            consume("=>") -> Token(STARSHIP, "<=>", pos)
            consume('=') -> Token(LESS_OR_EQUAL, "<=", pos)
            else -> Token(LESS_THAN, "<", pos)
        }

        '>' -> return when {
            consume('=') -> Token(GREATER_OR_EQUAL, ">=", pos)
            else -> Token(GREATER_THAN, ">", pos)
        }

        '?' -> return when {
            consume(':') -> Token(ELVIS, "?:", pos)
            consume('.') -> Token(SAFE_DOT, "?.", pos)
            else -> Token(QUESTION, "?", pos)
        }

        else -> return when {
            // all we have left are IDENTIFIER, NUMBER, STRING, and keywords (as subset of IDENTIFIER)

            Character.isJavaIdentifierStart(c) -> parseIdentOrKw(c, pos)
            c in '0'..'9' -> parseNumber(c, pos)
            c == '"' || c == '\'' -> parseString(c, pos, startPos)
            c == '`' -> lexTemplate(startPos, pos)
            else -> syntaxError("Unexpected character ${c.toPrintable()}", pos)
        }
    }
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
private fun CharStream.parseString(quote: Char, pos: Pos, startPos: Int): Token {
    val sb = StringBuilder()
    val otherQuote = if (quote == '"') '\'' else '"'

    while (moreChars()) {
        consumeInto(sb, isNotQuoteOrBackSlash)

        if (eof())
            syntaxError("Unterminated string", getPos())

        if (consume(quote)) {
            return Token(STRING, rawTextSince(startPos), pos, sb.toString())
        }

        if (consume(otherQuote)) {
            sb.append(otherQuote)
            continue
        }

        // must be backslash
        nextChar()
        doEscapeChar(sb)
    }

    syntaxError("Unterminated string", getPos())
}

val isDigit: (Char)->Boolean = { it in '0'..'9' }
private fun CharStream.parseNumber(firstChar: Char, pos: Pos): Token {
    val sb = StringBuilder()
    sb.append(firstChar)
    consumeInto(sb, isDigit)

    if (peek() == '.') {
        sb.append(nextChar())
        if (consumeInto(sb, isDigit) < 1) {
            putBack('.')
            sb.setLength(sb.length - 1)
            return Token(DOUBLE, sb.toString(), pos)
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
        return Token(DECIMAL, sb.toString(), pos, beforePrefix)
    } else {
        return Token(DOUBLE, sb.toString(), pos)
    }
}

val isIdentChar: (Char)->Boolean = Character::isJavaIdentifierPart
private fun CharStream.parseIdentOrKw(firstChar: Char, pos: Pos): Token {
    val sb = StringBuilder()
    sb.append(firstChar)
    consumeInto(sb, isIdentChar)
    val ident = sb.toString()

    val kwType = skriptKeywords[ident]
    if (kwType != null) {
        val str = internedKeywordStrings[kwType] ?: throw IllegalStateException()
        return Token(kwType, str, pos)
    } else {
        return Token(IDENTIFIER, ident, pos)
    }
}

fun isValidSkriptIdentifier(s: String): Boolean {
    if (s.isEmpty())
        return false

    if (!Character.isJavaIdentifierStart(s.get(0)))
        return false

    for (i in 1 until s.length) {
        if (!Character.isJavaIdentifierPart(s.get(i)))
            return false
    }

    return s !in skriptKeywords.keys
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

val notHash: (Char)->Boolean = { it != '#' }
private fun CharStream.consumeTemplateComment() {
    while (moreChars()) {
        skipWhile(notHash)
        if (consume('#') && consume('}'))
            return
    }
}