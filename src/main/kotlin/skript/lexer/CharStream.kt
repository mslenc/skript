package skript.lexer

interface CharStream {
    /**
     * Returns true while there are no more characters.
     */
    fun moreChars(): Boolean

    /**
     * Returns the next char, if any, but does not consume it. If there are no more chars, 0 is returned.
     */
    fun peek(): Char

    /**
     * Returns the next char, or 0 if EOF.
     */
    fun nextChar(): Char

    /**
     * Consumes the next codepoint if it matches the parameter, otherwise does nothing.
     */
    fun consume(opt: Char): Boolean {
        if (eof())
            return false

        return when (peek()) {
            opt -> { nextChar(); true }
            else -> false
        }
    }

    /**
     * Matches with the provided matcher while it returns true and returns the matched string.
     */
    fun consume(match: (Char)->Boolean): String {
        val sb = StringBuilder()
        consumeInto(sb, match)
        return sb.toString()
    }

    /**
     * Matches with the provided matcher while it returns true, copies the corresponding characters into sb, and returns the matched length.
     */
    fun consumeInto(sb: StringBuilder, match: (Char)->Boolean): Int {
        val start = sb.length

        loop@
        while (moreChars()) {
            val p = peek()
            when {
                match(p) -> sb.append(nextChar())
                else -> break@loop
            }
        }

        return sb.length - start
    }

    /**
     * Skips characters while they match and returns the skipped length.
     */
    fun skipWhile(match: (Char)->Boolean): Int {
        var result = 0

        loop@
        while (moreChars()) {
            val p = peek()
            when {
                match(p) -> { result++; nextChar() }
                else -> break@loop
            }
        }

        return result
    }

    /**
     * Retrieves the current position (row/column).
     */
    fun getPos(): Pos
}

fun CharStream.eof() = !moreChars()

class StringCharStream(private val str: String, private val fileName: String) : CharStream {
    private var pos = 0

    private var row = 1
    private var col = 1
    private var afterCR = false // did we just see \r ?
    private var countedUpTo = 0

    override fun moreChars(): Boolean {
        return pos < str.length
    }

    override fun getPos(): Pos {
        while (countedUpTo < pos) {
            when(str[countedUpTo++]) {
                '\r' -> {
                    row++
                    col = 1
                    afterCR = true
                }
                '\n' -> {
                    if (afterCR) {
                        afterCR = false
                    } else {
                        row++
                        col = 1
                    }
                }
                else -> {
                    afterCR = false
                    col++
                }
            }
        }

        return Pos(row, col, fileName)
    }

    override fun nextChar(): Char {
        return when {
            pos < str.length -> str[pos++]
            else -> 0.toChar()
        }
    }

    override fun peek(): Char {
        return when {
            pos < str.length -> str[pos]
            else -> 0.toChar()
        }
    }

    override fun consume(opt: Char): Boolean {
        return when {
            pos >= str.length -> false
            str[pos] == opt -> { pos++; true }
            else -> false
        }
    }

    override fun consume(match: (Char) -> Boolean): String {
        val start = pos
        val strLen = str.length

        while (pos < strLen) {
            if (match(str[pos])) {
                pos++
            } else {
                break
            }
        }

        return if (pos == start) {
            ""
        } else {
            str.substring(start, pos)
        }
    }

    override fun consumeInto(sb: StringBuilder, match: (Char) -> Boolean): Int {
        val start = pos
        val strLen = str.length

        while (pos < strLen) {
            val c = str[pos]
            if (match(c)) {
                pos++
                sb.append(c)
            } else {
                break
            }
        }

        return pos - start
    }

    override fun skipWhile(match: (Char) -> Boolean): Int {
        val start = pos
        val strLen = str.length

        while (pos < strLen && match(str[pos]))
            pos++

        return pos - start
    }
}