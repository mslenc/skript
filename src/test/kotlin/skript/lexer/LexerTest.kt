package skript.lexer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.lexer.TokenType.*
import kotlin.math.min

class LexerTest {
    @Test
    fun testBasics() {
        val filename = "testBasics.sk"
        
        val source = """
            import { foo, bar } from rosetta.stone; # comment?
            
            let a = 2 + 3 * 4 / 5 % 6 // 7 ** 8;
            
            "abc\"def" 12 12.34 12E5 12E-4 14E+2 12.34E+5 12.34E-5
            12.toFixed(5)
            12.emit()
            true += false 
            abc -= def*=bibi/=asd%=foo
            null**=undefined
            class//=fun
            super[this]
            var val for while if
            else when do return
            break continue
            try throw typeof
            ( ) { }
            . , : ;
            a in b || a !in b | a is C && Šlenc is ! is  & foo
            !a |= &= ||= &&=
            a == A === AAA
            a != b !== C
            a < b <= c <=> d >= e > f
            @here { a -> a - 1 } ?: nooo???
            1..5 true..<false
            ++a--
            a?b?.c:e
        """.trimIndent()

        val tokens = CharStream(source, filename).lex()

        val expect = listOf(
            Token(IDENTIFIER,         "import",         Pos( 1,  1, filename)),
            Token(LCURLY,             "{",              Pos( 1,  8, filename)),
            Token(IDENTIFIER,         "foo",            Pos( 1, 10, filename)),
            Token(COMMA,              ",",              Pos( 1, 13, filename)),
            Token(IDENTIFIER,         "bar",            Pos( 1, 15, filename)),
            Token(RCURLY,             "}",              Pos( 1, 19, filename)),
            Token(IDENTIFIER,         "from",           Pos( 1, 21, filename)),
            Token(IDENTIFIER,         "rosetta",        Pos( 1, 26, filename)),
            Token(DOT,                ".",              Pos( 1, 33, filename)),
            Token(IDENTIFIER,         "stone",          Pos( 1, 34, filename)),
            Token(SEMI,               ";",              Pos( 1, 39, filename)),

            Token(IDENTIFIER,         "let",            Pos( 3,  1, filename)),
            Token(IDENTIFIER,         "a",              Pos( 3,  5, filename)),
            Token(ASSIGN,             "=",              Pos( 3,  7, filename)),
            Token(NUMBER,             "2",              Pos( 3,  9, filename)),
            Token(PLUS,               "+",              Pos( 3, 11, filename)),
            Token(NUMBER,             "3",              Pos( 3, 13, filename)),
            Token(STAR,               "*",              Pos( 3, 15, filename)),
            Token(NUMBER,             "4",              Pos( 3, 17, filename)),
            Token(SLASH,              "/",              Pos( 3, 19, filename)),
            Token(NUMBER,             "5",              Pos( 3, 21, filename)),
            Token(PERCENT,            "%",              Pos( 3, 23, filename)),
            Token(NUMBER,             "6",              Pos( 3, 25, filename)),
            Token(SLASH_SLASH,        "//",             Pos( 3, 27, filename)),
            Token(NUMBER,             "7",              Pos( 3, 30, filename)),
            Token(STAR_STAR,          "**",             Pos( 3, 32, filename)),
            Token(NUMBER,             "8",              Pos( 3, 35, filename)),
            Token(SEMI,               ";",              Pos( 3, 36, filename)),

            Token(STRING,             "\"abc\\\"def\"", Pos( 5,  1, filename)),
            Token(NUMBER,             "12",             Pos( 5, 12, filename)),
            Token(NUMBER,             "12.34",          Pos( 5, 15, filename)),
            Token(NUMBER,             "12E5",           Pos( 5, 21, filename)),
            Token(NUMBER,             "12E-4",          Pos( 5, 26, filename)),
            Token(NUMBER,             "14E+2",          Pos( 5, 32, filename)),
            Token(NUMBER,             "12.34E+5",       Pos( 5, 38, filename)),
            Token(NUMBER,             "12.34E-5",       Pos( 5, 47, filename)),

            Token(NUMBER,             "12",             Pos( 6,  1, filename)),
            Token(DOT,                ".",              Pos( 6,  3, filename)),
            Token(IDENTIFIER,         "toFixed",        Pos( 6,  4, filename)),
            Token(LPAREN,             "(",              Pos( 6, 11, filename)),
            Token(NUMBER,             "5",              Pos( 6, 12, filename)),
            Token(RPAREN,             ")",              Pos( 6, 13, filename)),

            Token(NUMBER,             "12",             Pos( 7,  1, filename)),
            Token(DOT,                ".",              Pos( 7,  3, filename)),
            Token(IDENTIFIER,         "emit",           Pos( 7,  4, filename)),
            Token(LPAREN,             "(",              Pos( 7,  8, filename)),
            Token(RPAREN,             ")",              Pos( 7,  9, filename)),

            Token(TRUE,               "true",           Pos( 8,  1, filename)),
            Token(PLUS_ASSIGN,        "+=",             Pos( 8,  6, filename)),
            Token(FALSE,              "false",          Pos( 8,  9, filename)),

            Token(IDENTIFIER,         "abc",            Pos( 9,  1, filename)),
            Token(MINUS_ASSIGN,       "-=",             Pos( 9,  5, filename)),
            Token(IDENTIFIER,         "def",            Pos( 9,  8, filename)),
            Token(STAR_ASSIGN,        "*=",             Pos( 9, 11, filename)),
            Token(IDENTIFIER,         "bibi",           Pos( 9, 13, filename)),
            Token(SLASH_ASSIGN,       "/=",             Pos( 9, 17, filename)),
            Token(IDENTIFIER,         "asd",            Pos( 9, 19, filename)),
            Token(PERCENT_ASSIGN,     "%=",             Pos( 9, 22, filename)),
            Token(IDENTIFIER,         "foo",            Pos( 9, 24, filename)),

            Token(NULL,               "null",           Pos(10,  1, filename)),
            Token(STAR_STAR_ASSIGN,   "**=",            Pos(10,  5, filename)),
            Token(UNDEFINED,          "undefined",      Pos(10,  8, filename)),

            Token(CLASS,              "class",          Pos(11,  1, filename)),
            Token(SLASH_SLASH_ASSIGN, "//=",            Pos(11,  6, filename)),
            Token(FUNCTION,           "fun",            Pos(11,  9, filename)),

            Token(SUPER,              "super",          Pos(12,  1, filename)),
            Token(LBRACK,             "[",              Pos(12,  6, filename)),
            Token(THIS,               "this",           Pos(12,  7, filename)),
            Token(RBRACK,             "]",              Pos(12, 11, filename)),

            Token(VAR,                "var",            Pos(13,  1, filename)),
            Token(VAL,                "val",            Pos(13,  5, filename)),
            Token(FOR,                "for",            Pos(13,  9, filename)),
            Token(WHILE,              "while",          Pos(13, 13, filename)),
            Token(IF,                 "if",             Pos(13, 19, filename)),

            Token(ELSE,               "else",           Pos(14,  1, filename)),
            Token(WHEN,               "when",           Pos(14,  6, filename)),
            Token(DO,                 "do",             Pos(14, 11, filename)),
            Token(RETURN,             "return",         Pos(14, 14, filename)),

            Token(BREAK,              "break",          Pos(15,  1, filename)),
            Token(CONTINUE,           "continue",       Pos(15,  7, filename)),

            Token(TRY,                "try",            Pos(16,  1, filename)),
            Token(THROW,              "throw",          Pos(16,  5, filename)),
            Token(TYPEOF,             "typeof",         Pos(16, 11, filename)),

            Token(LPAREN,             "(",              Pos(17,  1, filename)),
            Token(RPAREN,             ")",              Pos(17,  3, filename)),
            Token(LCURLY,             "{",              Pos(17,  5, filename)),
            Token(RCURLY,             "}",              Pos(17,  7, filename)),

            Token(DOT,                ".",              Pos(18,  1, filename)),
            Token(COMMA,              ",",              Pos(18,  3, filename)),
            Token(COLON,              ":",              Pos(18,  5, filename)),
            Token(SEMI,               ";",              Pos(18,  7, filename)),

            Token(IDENTIFIER,         "a",              Pos(19,  1, filename)),
            Token(IN,                 "in",             Pos(19,  3, filename)),
            Token(IDENTIFIER,         "b",              Pos(19,  6, filename)),
            Token(OR_OR,              "||",             Pos(19,  8, filename)),
            Token(IDENTIFIER,         "a",              Pos(19, 11, filename)),
            Token(NOT_IN,             "!in",            Pos(19, 13, filename)),
            Token(IDENTIFIER,         "b",              Pos(19, 17, filename)),
            Token(OR,                 "|",              Pos(19, 19, filename)),
            Token(IDENTIFIER,         "a",              Pos(19, 21, filename)),
            Token(IS,                 "is",             Pos(19, 23, filename)),
            Token(IDENTIFIER,         "C",              Pos(19, 26, filename)),
            Token(AND_AND,            "&&",             Pos(19, 28, filename)),
            Token(IDENTIFIER,         "Šlenc",          Pos(19, 31, filename)),
            Token(IS,                 "is",             Pos(19, 37, filename)),
            Token(NOT_IS,             "!is",            Pos(19, 40, filename)),
            Token(AND,                "&",              Pos(19, 46, filename)),
            Token(IDENTIFIER,         "foo",            Pos(19, 48, filename)),

            Token(EXCL,               "!",              Pos(20,  1, filename)),
            Token(IDENTIFIER,         "a",              Pos(20,  2, filename)),
            Token(OR_ASSIGN,          "|=",             Pos(20,  4, filename)),
            Token(AND_ASSIGN,         "&=",             Pos(20,  7, filename)),
            Token(OR_OR_ASSIGN,       "||=",            Pos(20, 10, filename)),
            Token(AND_AND_ASSIGN,     "&&=",            Pos(20, 14, filename)),

            Token(IDENTIFIER,         "a",              Pos(21,  1, filename)),
            Token(EQUALS,             "==",             Pos(21,  3, filename)),
            Token(IDENTIFIER,         "A",              Pos(21,  6, filename)),
            Token(STRICT_EQUALS,      "===",            Pos(21,  8, filename)),
            Token(IDENTIFIER,         "AAA",            Pos(21, 12, filename)),

            Token(IDENTIFIER,         "a",              Pos(22,  1, filename)),
            Token(NOT_EQUAL,          "!=",             Pos(22,  3, filename)),
            Token(IDENTIFIER,         "b",              Pos(22,  6, filename)),
            Token(NOT_STRICT_EQUAL,   "!==",            Pos(22,  8, filename)),
            Token(IDENTIFIER,         "C",              Pos(22, 12, filename)),

            Token(IDENTIFIER,         "a",              Pos(23,  1, filename)),
            Token(LESS_THAN,          "<",              Pos(23,  3, filename)),
            Token(IDENTIFIER,         "b",              Pos(23,  5, filename)),
            Token(LESS_OR_EQUAL,      "<=",             Pos(23,  7, filename)),
            Token(IDENTIFIER,         "c",              Pos(23, 10, filename)),
            Token(STARSHIP,           "<=>",            Pos(23, 12, filename)),
            Token(IDENTIFIER,         "d",              Pos(23, 16, filename)),
            Token(GREATER_OR_EQUAL,   ">=",             Pos(23, 18, filename)),
            Token(IDENTIFIER,         "e",              Pos(23, 21, filename)),
            Token(GREATER_THAN,       ">",              Pos(23, 23, filename)),
            Token(IDENTIFIER,         "f",              Pos(23, 25, filename)),

            Token(AT,                 "@",              Pos(24,  1, filename)),
            Token(IDENTIFIER,         "here",           Pos(24,  2, filename)),
            Token(LCURLY,             "{",              Pos(24,  7, filename)),
            Token(IDENTIFIER,         "a",              Pos(24,  9, filename)),
            Token(ARROW,              "->",             Pos(24, 11, filename)),
            Token(IDENTIFIER,         "a",              Pos(24, 14, filename)),
            Token(MINUS,              "-",              Pos(24, 16, filename)),
            Token(NUMBER,             "1",              Pos(24, 18, filename)),
            Token(RCURLY,             "}",              Pos(24, 20, filename)),
            Token(ELVIS,              "?:",             Pos(24, 22, filename)),
            Token(IDENTIFIER,         "nooo",           Pos(24, 25, filename)),
            Token(QUESTION,           "?",              Pos(24, 29, filename)),
            Token(QUESTION,           "?",              Pos(24, 30, filename)),
            Token(QUESTION,           "?",              Pos(24, 31, filename)),

            Token(NUMBER,             "1",              Pos(25, 1, filename)),
            Token(DOT_DOT,            "..",             Pos(25, 2, filename)),
            Token(NUMBER,             "5",              Pos(25, 4, filename)),
            Token(TRUE,               "true",           Pos(25, 6, filename)),
            Token(DOT_DOT_LESS,       "..<",            Pos(25, 10, filename)),
            Token(FALSE,              "false",          Pos(25, 13, filename)),

            Token(PLUS_PLUS,          "++",             Pos(26, 1, filename)),
            Token(IDENTIFIER,         "a",              Pos(26, 3, filename)),
            Token(MINUS_MINUS,        "--",             Pos(26, 4, filename)),

            // a?b?.c:e
            Token(IDENTIFIER,         "a",              Pos(27, 1, filename)),
            Token(QUESTION,           "?",              Pos(27, 2, filename)),
            Token(IDENTIFIER,         "b",              Pos(27, 3, filename)),
            Token(SAFE_DOT,           "?.",             Pos(27, 4, filename)),
            Token(IDENTIFIER,         "c",              Pos(27, 6, filename)),
            Token(COLON,              ":",              Pos(27, 7, filename)),
            Token(IDENTIFIER,         "e",              Pos(27, 8, filename)),

            Token(EOF,                "",               Pos(27, 9, filename))
        )

        for (i in 0 until min(tokens.size, expect.size))
            assertEquals(expect[i], tokens[i])

        assertEquals(expect.size, tokens.size)

        val remainingTokens = enumValues<TokenType>().toMutableSet()
        for (token in tokens)
            remainingTokens.remove(token.type)

        assertTrue(remainingTokens.isEmpty()) { remainingTokens.toString() }

        // speed also seems ok - 10_000_000 runs in 70 seconds, or about 7 us / run,
        // or about 140k runs / second.. good enough for intended purposes..
    }
}