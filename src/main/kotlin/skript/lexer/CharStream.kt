package skript.lexer

class CharStream(private val str: String, private val fileName: String) {
    private var pos = 0

    private var row = 1
    private var col = 1
    private var afterCR = false // did we just see \r ?
    private var countedUpTo = 0

    /**
     * Returns true while there are more characters.
     */
    fun moreChars(): Boolean {
        return pos < str.length
    }

    /**
     * Returns true if there are no more characters.
     */
    fun eof(): Boolean {
        return pos >= str.length
    }

    /**
     * Retrieves the current position (row/column).
     */
    fun getPos(): Pos {
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

    /**
     * Returns the next char, or 0 if EOF.
     */
    fun nextChar(): Char {
        return when {
            pos < str.length -> str[pos++]
            else -> 0.toChar()
        }
    }

    /**
     * Returns the next char, if any, but does not consume it. If there are no more chars, 0 is returned.
     */
    fun peek(): Char {
        return when {
            pos < str.length -> str[pos]
            else -> 0.toChar()
        }
    }

    fun putBack(c: Char) {
        check(pos > 0 && str[pos - 1] == c) { "putBack called with wrong character" }

        --pos
    }

    /**
     * Consumes the next char if it matches the parameter, otherwise does nothing.
     */
    fun consume(opt: Char): Boolean {
        return when {
            pos >= str.length -> false
            str[pos] == opt -> { pos++; true }
            else -> false
        }
    }

    /**
     * Matches with the provided matcher while it returns true, copies the corresponding characters into sb, and returns the matched length.
     */
    fun consumeInto(sb: StringBuilder, match: (Char) -> Boolean): Int {
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

    /**
     * Skips characters while they match and returns the skipped length.
     */
    fun skipWhile(match: (Char) -> Boolean): Int {
        val start = pos
        val strLen = str.length

        while (pos < strLen && match(str[pos]))
            pos++

        return pos - start
    }
}