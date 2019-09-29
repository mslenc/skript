package skript.parser

import skript.ast.*
import skript.parser.TokenType.*
import skript.syntaxError
import skript.values.*

fun Tokens.parseExpression(): Expression {
    return parseAssignment()
}

fun Tokens.parseAssignment(): Expression {
    val left = parseTernary()

    if (left !is LValue)
        return left

    val nextTok = peek()

    val binaryOp = when {
        nextTok.type == ASSIGN -> null
        else -> nextTok.type.toAssignOp() ?: return left
    }
    next()

    val value = parseAssignment()

    return AssignExpression(left, binaryOp, value)
}

fun TokenType.toAssignOp(): BinaryOp? = when (this) {
    PLUS_ASSIGN -> BinaryOp.ADD
    MINUS_ASSIGN -> BinaryOp.SUBTRACT
    STAR_ASSIGN -> BinaryOp.MULTIPLY
    SLASH_ASSIGN -> BinaryOp.DIVIDE
    SLASH_SLASH_ASSIGN -> BinaryOp.DIVIDE_INT
    PERCENT_ASSIGN -> BinaryOp.REMAINDER
    // TODO: STAR_STAR_ASSIGN -> BinaryOp.EXPONENT
    OR_ASSIGN -> BinaryOp.OR
    AND_ASSIGN -> BinaryOp.AND
    OR_OR_ASSIGN -> BinaryOp.OR_OR
    AND_AND_ASSIGN -> BinaryOp.AND_AND
    else -> null
}

fun Tokens.parseTernary(): Expression {
    val condition = parseDisjunction()

    return if (consume(QUESTION) != null) {
        val ifTrue = parseAssignment()
        expect(COLON)
        val ifFalse = parseAssignment()
        TernaryExpression(condition, ifTrue, ifFalse)
    } else {
        condition
    }
}

fun Tokens.parseDisjunction(): Expression {
    var result = parseConjunction()
    while (true) {
        result = when (peek().type) {
            OR ->    BinaryExpression(next().pos, result, BinaryOp.OR,    parseConjunction())
            OR_OR -> BinaryExpression(next().pos, result, BinaryOp.OR_OR, parseConjunction())
            else -> return result
        }
    }
}

fun Tokens.parseConjunction(): Expression {
    var result = parseComparison()
    while (true) {
        result = when (peek().type) {
            AND ->     BinaryExpression(next().pos, result, BinaryOp.AND,     parseComparison())
            AND_AND -> BinaryExpression(next().pos, result, BinaryOp.AND_AND, parseComparison())
            else -> return result
        }
    }
}

fun TokenType.toComparisonOp(): BinaryOp? = when (this) {
    LESS_THAN -> BinaryOp.LESS_THAN
    LESS_OR_EQUAL -> BinaryOp.LESS_OR_EQUAL
    GREATER_THAN -> BinaryOp.GREATER_THAN
    GREATER_OR_EQUAL -> BinaryOp.GREATER_OR_EQUAL
    EQUALS -> BinaryOp.EQUALS
    NOT_EQUAL -> BinaryOp.NOT_EQUALS
    STRICT_EQUALS -> BinaryOp.STRICT_EQUALS
    NOT_STRICT_EQUAL -> BinaryOp.NOT_STRICT_EQUALS
    STARSHIP -> BinaryOp.STARSHIP

    else -> null
}

// these combos are valid and combine sequentially: // a <= b < c <= d == e < f ...     => (a <= b) && (b < c) && (c <= d) ...
val validComparisonSetsSeq = listOf(
    setOf(LESS_THAN, LESS_OR_EQUAL, EQUALS),
    setOf(GREATER_THAN, GREATER_OR_EQUAL, EQUALS),
    setOf(STRICT_EQUALS)
    // setOf(EQUALS) is not needed, because it is contained in the first two anyway
).map { it.map { tt -> tt.toComparisonOp()!! }.toSet() }

// these "combos" are valid and combine all pairs: a !== b !== c => (a !== b) && (a !== c) && (b !== c)
val validComparisonSetsPairwise = listOf(
    setOf(NOT_EQUAL),           // a != b != c != d    => (a != b) && (a != c) && (b != c) && (a != d) && (b != d) && (c != d)
    setOf(NOT_STRICT_EQUAL)     //
).map { it.map { tt -> tt.toComparisonOp()!! }.toSet() }

// also note that there is no setOf(STARSHIP) - it can't be in a sequence longer than a single operator

fun Tokens.parseComparison(): Expression {
    // we avoid allocating the lists for 0 and 1 operator, because those are expected to be the vast majority, and also
    // to allow STARSHIP
    // (but we do need the lists in bigger cases, because we only allow sequences where all operators are compatible, listed above)

    val first = parseInIs()
    val pos = peek().pos
    val firstOp = peek().type.toComparisonOp()?.also { next() } ?: return first
    val second = parseInIs()
    val secondOp = peek().type.toComparisonOp()?.also { next() } ?: return BinaryExpression(pos, first, firstOp, second)

    val parts = mutableListOf(first, second, parseInIs())
    val ops = mutableListOf(firstOp, secondOp)

    while (true) {
        ops += peek().type.toComparisonOp()?.also { next() } ?: break
        parts += parseInIs()
    }

    val opsSeen = ops.toSet()

    for (validOption in validComparisonSetsSeq) {
        if (validOption.containsAll(opsSeen)) {
            return CompareSequence(parts, ops)
        }
    }

    for (validOption in validComparisonSetsPairwise) {
        if (validOption.containsAll(opsSeen)) {
            return CompareAllPairs(parts, ops)
        }
    }

    syntaxError("Can't combine these operators (${ opsSeen })", pos)
}

fun Tokens.parseInIs(): Expression {
    var result = parseElvis()

    while (true) {
        val nextTok = peek().type

        result = when (nextTok) {
            IN ->     { next(); ValueIn(result, parseElvis(), true) }
            NOT_IN -> { next(); ValueIn(result, parseElvis(), false) }

            IS ->     { next(); val ident = expect(IDENTIFIER); ObjectIs(result, Variable(ident.rawText, ident.pos), true) }
            NOT_IS -> { next(); val ident = expect(IDENTIFIER); ObjectIs(result, Variable(ident.rawText, ident.pos), false) }

            else -> return result
        }
    }
}

fun Tokens.parseElvis(): Expression {
    var result = parseInfixCall()
    while (true) {
        result = when (peek().type) {
            ELVIS -> BinaryExpression(next().pos, result, BinaryOp.ELVIS, parseInfixCall())
            else -> return result
        }
    }
}

fun Tokens.parseInfixCall(): Expression {
    var result = parseRange()
    while (true) {
        if (peekOnNewLine() || peek().type != IDENTIFIER)
            return result
        result = MethodCall(result, next().rawText, listOf(PosArg(parseRange())), MethodCallType.INFIX)
    }
}

fun Tokens.parseRange(): Expression {
    var result = parseAdd()
    while (true) {
        result = when (peek().type) {
            DOT_DOT ->      BinaryExpression(next().pos, result, BinaryOp.RANGE_TO,      parseAdd())
            DOT_DOT_LESS -> BinaryExpression(next().pos, result, BinaryOp.RANGE_TO_EXCL, parseAdd())
            else -> return result
        }
    }
}

fun Tokens.parseAdd(): Expression {
    var result = parseMul()
    while (true) {
        result = when (peek().type) {
            PLUS ->  BinaryExpression(next().pos, result, BinaryOp.ADD,      parseMul())
            MINUS -> BinaryExpression(next().pos, result, BinaryOp.SUBTRACT, parseMul())
            else -> return result
        }
    }
}

fun Tokens.parseMul(): Expression {
    var result = parsePower()
    while (true) {
        result = when (peek().type) {
            STAR ->        BinaryExpression(next().pos, result, BinaryOp.MULTIPLY,   parsePower())
            SLASH ->       BinaryExpression(next().pos, result, BinaryOp.DIVIDE,     parsePower())
            SLASH_SLASH -> BinaryExpression(next().pos, result, BinaryOp.DIVIDE_INT, parsePower())
            PERCENT ->     BinaryExpression(next().pos, result, BinaryOp.REMAINDER,  parsePower())
            else -> return result
        }
    }
}

fun Tokens.parsePower(): Expression {
    var result = parsePrefixUnary()
    while (true) {
        result = when (peek().type) {
            STAR_STAR -> BinaryExpression(next().pos, result, BinaryOp.POWER, parsePower())
            else -> return result
        }
    }
}

fun Tokens.parsePrefixUnary(): Expression {
    return when (peek().type) {
        MINUS -> { next(); UnaryExpression(UnaryOp.MINUS, parsePrefixUnary()) }
        PLUS -> { next(); UnaryExpression(UnaryOp.PLUS, parsePrefixUnary()) }
        EXCL -> { next(); UnaryExpression(UnaryOp.NOT, parsePrefixUnary()) }
        PLUS_PLUS -> {
            val tok = next()
            val inner = parsePrefixUnary()
            if (inner is LValue) {
                PrePostExpr(PrePostOp.PRE_INCR, inner)
            } else {
                syntaxError("Pre-increment ++ can only be used on lvalues", tok.pos)
            }
        }
        MINUS_MINUS -> {
            val tok = next()
            val inner = parsePrefixUnary()
            if (inner is LValue) {
                PrePostExpr(PrePostOp.PRE_DECR, inner)
            } else {
                syntaxError("Pre-decrement -- can only be used on lvalues", tok.pos)
            }
        }
        else -> parsePostfixUnary()
    }
}

fun Tokens.parsePostfixUnary(): Expression {
    return parsePostfixes(parsePrimary())
}

fun Tokens.parsePostfixes(primary: Expression): Expression {
    var result = primary

    while (true) {
        if (peekOnNewLine() && peek().type != SAFE_DOT && peek().type != DOT) // all others can start a new statement
            return result

        result = when (peek().type) {
            PLUS_PLUS -> {
                val tok = next()
                if (result is LValue) {
                    PrePostExpr(PrePostOp.POST_INCR, result)
                } else {
                    syntaxError("Post-increment ++ can only be used on lvalues", tok.pos)
                }
            }
            MINUS_MINUS -> {
                val tok = next()
                if (result is LValue) {
                    PrePostExpr(PrePostOp.POST_DECR, result)
                } else {
                    syntaxError("Post-decrement -- can only be used on lvalues", tok.pos)
                }
            }
            SAFE_DOT,
            DOT -> {
                val callType = if (next().type == SAFE_DOT) MethodCallType.SAFE else MethodCallType.REGULAR
                val ident = expect(IDENTIFIER)
                if (peek().type == LPAREN) {
                    MethodCall(result, ident.rawText, parseCallArgs(), callType)
                } else {
                    PropertyAccess(result, ident.rawText)
                }
            }
            LBRACK -> {
                next()
                val index = parseExpression()
                expect(RBRACK)
                ElementAccess(result, index)
            }
            LPAREN -> {
                FuncCall(result, parseCallArgs())
            }
            else -> return result
        }
    }
}

fun Tokens.parseCallArgs(): List<CallArg> {
    val args = ArrayList<CallArg>()
    val namesSeen = HashSet<String>()

    var kwOnly = false

    expect(LPAREN)
    while (true) {
        val peeked = peek()
        when (peeked.type) {
            RPAREN -> {
                next()
                return args
            }
            STAR -> {
                if (kwOnly) {
                    syntaxError("Positional arguments must come before keyword arguments", peeked.pos)
                } else {
                    next()
                    args += SpreadPosArg(parseExpression())
                }
            }
            STAR_STAR -> {
                next()
                kwOnly = true
                args += SpreadKwArg(parseExpression())
            }
            else -> {
                val arg = parseExpression()
                if (arg is AssignExpression && arg.op == null && arg.left is Variable) {
                    if (namesSeen.add(arg.left.varName)) {
                        args += KwArg(arg.left.varName, arg.right)
                        kwOnly = true
                    } else {
                        syntaxError("Argument named ${ arg.left.varName } was already defined", arg.left.pos)
                    }
                } else {
                    if (kwOnly) {
                        syntaxError("Positional arguments must come before keyword arguments", peeked.pos)
                    }
                    args += PosArg(arg)
                }
            }
        }

        if (peek().type != RPAREN) {
            expect(COMMA)
        }
    }
}

fun Tokens.parsePrimary(): Expression {
    if (peek().type == EOF)
        syntaxError("Expected an expression, not <EOF>", peek().pos)

    val tok = next()

    return when (tok.type) {
        TRUE -> { Literal(SkBoolean.TRUE) }
        FALSE -> { Literal(SkBoolean.FALSE) }
        NULL -> { Literal(SkNull) }
        UNDEFINED -> { Literal(SkUndefined) }
        DOUBLE -> { Literal(SkDouble.valueOf(tok.rawText.toDoubleOrNull() ?: syntaxError("Couldn't parse double ${tok.rawText}", tok.pos))) }
        DECIMAL -> { Literal(SkDecimal.valueOf(tok.value.toString().toBigDecimalOrNull() ?: syntaxError("Couldn't parse decimal ${tok.value}", tok.pos))) }
        STRING -> { Literal(SkString(tok.value.toString())) }

        LPAREN -> {
            val result = parseExpression()
            expect(RPAREN)
            return Parentheses(result)
        }

        IDENTIFIER -> {
            return Variable(tok.rawText, tok.pos)
        }

        FUNCTION -> {
            return FunctionLiteral(parseFunctionDecl(funLiteral = true))
        }

        LBRACK -> {
            return parseRestOfListLiteral()
        }

        LCURLY -> {
            return parseRestOfMapLiteral()
        }

        TEMPLATE -> {
            return processStringTemplate(tok)
        }

        else -> {
            syntaxError("Unexpected token ${tok.type}", tok.pos)
        }
    }
}

fun processStringTemplate(token: Token): StringTemplateExpr {
    val parts = (token.value as List<TemplatePart>).map { part ->
        when (part) {
            is TemplatePartString -> {
                StrTemplateText(part.text)
            }
            is TemplatePartExpr -> {
                val tokens = Tokens(part.tokens)
                val expr = tokens.parseExpression()
                if (tokens.peek().type != EOF) {
                    syntaxError("Unexpected token", tokens.peek().pos)
                } else {
                    StrTemplateExpr(expr)
                }
            }
        }
    }

    return StringTemplateExpr(parts)
}

fun Tokens.parseRestOfMapLiteral(): MapLiteral {
    val parts = ArrayList<MapLiteralPart>()

    while (true) {
        parts += when {
            consume(RCURLY) != null -> {
                return MapLiteral(parts)
            }

            consume(STAR_STAR) != null -> {
                val spread = parseExpression()
                MapLiteralPartSpread(spread)
            }

            consume(LBRACK) != null -> {
                val key = parseExpression()
                expect(RBRACK)
                expect(COLON)
                val value = parseExpression()
                MapLiteralPartExprKey(key, value)
            }

            else -> {
                val ident = expect(IDENTIFIER)
                expect(COLON)
                val value = parseExpression()
                MapLiteralPartFixedKey(ident.rawText, value)
            }
        }

        if (peek().type != RCURLY)
            expect(COMMA)
    }
}

fun Tokens.parseRestOfListLiteral(): ListLiteral {
    val parts = ArrayList<ListLiteralPart>()

    while (true) {
        parts += when {
            consume(RBRACK) != null ->
                return ListLiteral(parts)

            consume(STAR) != null ->
                ListLiteralPart(parseExpression(), isSpread = true)

            else ->
                ListLiteralPart(parseExpression(), isSpread = false)
        }

        if (peek().type != RBRACK)
            expect(COMMA)
    }
}