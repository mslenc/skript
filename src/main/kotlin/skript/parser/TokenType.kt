package skript.parser

enum class TokenType {
    IDENTIFIER,
    STRING, DOUBLE, DECIMAL, TRUE, FALSE, NULL, UNDEFINED,
    CLASS, FUNCTION, SUPER, THIS,
    VAR, VAL,
    FOR, WHILE, IF, ELSE, WHEN, DO, RETURN,
    BREAK, CONTINUE,
    TRY, THROW, TYPEOF,

    LPAREN, RPAREN,                      // ( )
    LBRACK, RBRACK,                      // [ ]
    LCURLY, RCURLY,                      // { }
    DOT, COMMA, COLON, SEMI,             // . , : ;
    EXCL, IN, NOT_IN, IS, NOT_IS,         // ! in !in is !is
    PLUS, MINUS, STAR, SLASH, PERCENT,   // + - * / %
    PLUS_PLUS, MINUS_MINUS,              // ++ --
    STAR_STAR, SLASH_SLASH,              // ** //
    ASSIGN, EQUALS, STRICT_EQUALS,       // = == ===
    NOT_EQUAL, NOT_STRICT_EQUAL,         // != !==
    LESS_THAN, LESS_OR_EQUAL,            // < <=
    GREATER_THAN, GREATER_OR_EQUAL,      // > >=
    QUESTION, ELVIS, SAFE_DOT,           // ? ?: ?.
    STARSHIP, AT, ARROW,                 // <=> @ ->
    OR, AND, OR_OR, AND_AND,             // | & || &&
    DOT_DOT, DOT_DOT_LESS,               // .. ..<

    PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN, PERCENT_ASSIGN, // += -= *= /= %=
    STAR_STAR_ASSIGN, SLASH_SLASH_ASSIGN,                                 // **= //=
    OR_ASSIGN, AND_ASSIGN, OR_OR_ASSIGN, AND_AND_ASSIGN,                  // |= &= ||= &&=

    TEMPLATE,

    EOF
}

val skriptKeywords = mapOf(
    "true" to TokenType.TRUE,
    "false" to TokenType.FALSE,
    "null" to TokenType.NULL,
    "undefined" to TokenType.UNDEFINED,
    "class" to TokenType.CLASS,
    "fun" to TokenType.FUNCTION,
    "super" to TokenType.SUPER,
    "this" to TokenType.THIS,
    "var" to TokenType.VAR,
    "val" to TokenType.VAL,
    "for" to TokenType.FOR,
    "while" to TokenType.WHILE,
    "if" to TokenType.IF,
    "else" to TokenType.ELSE,
    "when" to TokenType.WHEN,
    "do" to TokenType.DO,
    "return" to TokenType.RETURN,
    "break" to TokenType.BREAK,
    "continue" to TokenType.CONTINUE,
    "try" to TokenType.TRY,
    "throw" to TokenType.THROW,
    "typeof" to TokenType.TYPEOF,
    "in" to TokenType.IN,
    "is" to TokenType.IS
)

val internedKeywords = skriptKeywords.map { (str, value) -> value to str }.toMap()