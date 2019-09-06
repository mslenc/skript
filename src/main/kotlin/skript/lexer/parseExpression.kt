package skript.lexer

import skript.ast.*
import skript.lexer.TokenType.*
import skript.syntaxError
import skript.values.SkBoolean
import skript.values.SkNull
import skript.values.SkString
import skript.values.SkUndefined

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

private inline fun Tokens.parseLeftAssoc(parseSub: Tokens.()->Expression, tokenMatch: (TokenType)->Boolean, combine: (Expression, Token, Expression)->Expression): Expression {
    var result = parseSub()
    while (tokenMatch(peek().type)) {
        val opToken = next()
        val right = parseSub()
        result = combine(result, opToken, right)
    }
    return result
}

fun Tokens.parseDisjunction() = parseLeftAssoc({ parseConjunction() }, { it == OR_OR || it == OR }) { l, opTok, r ->
    val binaryOp = if (opTok.type == OR_OR) BinaryOp.OR_OR else BinaryOp.OR
    BinaryExpression(l, binaryOp, r)
}

fun Tokens.parseConjunction() = parseLeftAssoc({ parseComparison() }, { it == AND_AND || it == AND }) { l, opTok, r ->
    val binaryOp = if (opTok.type == AND_AND) BinaryOp.AND_AND else BinaryOp.AND
    BinaryExpression(l, binaryOp, r)
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
    val secondOp = peek().type.toComparisonOp()?.also { next() } ?: return BinaryExpression(first, firstOp, second)

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

            IS ->     { next(); ObjectIs(result, Variable(expect(IDENTIFIER).rawText), true) }
            NOT_IS -> { next(); ObjectIs(result, Variable(expect(IDENTIFIER).rawText), false) }

            else -> return result
        }
    }
}

fun Tokens.parseElvis() = parseLeftAssoc({ parseInfixCall() }, { it == ELVIS }) { l, _, r -> BinaryExpression(l, BinaryOp.ELVIS, r) }

fun Tokens.parseInfixCall() = parseLeftAssoc({ parseRangeExpression() }, { it == IDENTIFIER }) { l, ident, r ->
    MethodCall(l, ident.rawText, listOf(PosArg(r)), MethodCallType.INFIX)
}

fun Tokens.parseRangeExpression() = parseLeftAssoc({ parseAdd() }, { it == DOT_DOT || it == DOT_DOT_LESS }) { l, op, r ->
    val binaryOp = if (op.type == DOT_DOT_LESS) BinaryOp.RANGE_TO_EXCL else BinaryOp.RANGE_TO
    return BinaryExpression(l, binaryOp, r)
}

fun Tokens.parseAdd() = parseLeftAssoc({ parseMul() }, { it == PLUS || it == MINUS }) { l, op, r ->
    val binaryOp = if (op.type == PLUS) BinaryOp.ADD else BinaryOp.SUBTRACT
    return BinaryExpression(l, binaryOp, r)
}

fun Tokens.parseMul() = parseLeftAssoc({ parsePrefixUnary() }, { it == STAR || it == SLASH || it == SLASH_SLASH || it == PERCENT }) { l, op, r ->
    val binaryOp = when (op.type) {
        STAR -> BinaryOp.MULTIPLY
        SLASH -> BinaryOp.DIVIDE
        SLASH_SLASH -> BinaryOp.DIVIDE_INT
        else -> BinaryOp.REMAINDER
    }
    return BinaryExpression(l, binaryOp, r)
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
                    FieldAccess(result, ident.rawText)
                }
            }
            LBRACK -> {
                next()
                val index = parseExpression()
                expect(RBRACK)
                ArrayAccess(result, index)
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

    expect(LPAREN)
    while (true) {
        when (peek().type) {
            RPAREN -> {
                next()
                return args
            }
            STAR -> {
                next()
                args += SpreadPosArg(parseExpression())
            }
            STAR_STAR -> {
                next()
                args += SpreadKwArg(parseExpression())
            }
            else -> {
                val arg = parseExpression()
                if (arg is Variable && peek().type == ASSIGN) {
                    val tok = next()
                    if (namesSeen.add(arg.varName)) {
                        args += KwArg(arg.varName, parseExpression())
                    } else {
                        syntaxError("This name is already present", tok.pos)
                    }
                } else {
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
        NUMBER -> { Literal(SkString(tok.rawText).asNumber()) }
        STRING -> { Literal(SkString(tok.unescapeString())) }

        LPAREN -> {
            val result = parseExpression()
            expect(RPAREN)
            return Parentheses(result)
        }

        IDENTIFIER -> {
            return Variable(tok.rawText)
        }

        FUNCTION -> {
            return FunctionLiteral(parseFunctionDecl(funLiteral = true))
        }

        LBRACK -> {
            return parseRestOfMapOrListLiteral()
        }

        LCURLY -> {
            syntaxError("Lambdas not supported (yet)", tok.pos)
        }

        else -> {
            syntaxError("Unexpected token ${tok.type}", tok.pos)
        }
    }
}

fun Tokens.parseRestOfMapOrListLiteral(): Expression {
    val first = peek().type

    return when (first) {
        COLON -> {
            next()
            expect(RBRACK)
            MapLiteral(emptyList())
        }

        RBRACK -> {
            next()
            ListLiteral(emptyList())
        }

        STAR -> {
            parseRestOfListLiteral(null)
        }

        STAR_STAR -> {
            parseRestOfMapLiteral(null)
        }

        IDENTIFIER -> {
            val ident = next()
            if (consume(COLON) != null) {
                val value = parseExpression()
                consume(COMMA)
                parseRestOfMapLiteral(MapLiteralPartFixedKey(ident.rawText, value))
            } else {
                val element = parsePostfixes(Variable(ident.rawText))
                consume(COMMA)
                parseRestOfListLiteral(ListLiteralPart(element, false))
            }
        }

        LPAREN -> {
            val key = parseExpression()
            expect(RPAREN)

            if (consume(COLON) != null) {
                val value = parseExpression()
                consume(COMMA)
                parseRestOfMapLiteral(MapLiteralPartExprKey(key, value))
            } else {
                val element = parsePostfixes(key)
                consume(COMMA)
                parseRestOfListLiteral(ListLiteralPart(element, isSpread = false))
            }
        }

        else -> {
            parseRestOfListLiteral(null)
        }
    }
}

fun Tokens.parseRestOfMapLiteral(first: MapLiteralPart?): MapLiteral {
    val parts = ArrayList<MapLiteralPart>()
    first?.let { parts += it }

    while (true) {
        parts += when {
            consume(RBRACK) != null -> {
                return MapLiteral(parts)
            }

            consume(STAR_STAR) != null -> {
                val spread = parseExpression()
                MapLiteralPartSpread(spread)
            }

            consume(LPAREN) != null -> {
                val key = parseExpression()
                expect(RPAREN)
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

        consume(COMMA)
    }
}

fun Tokens.parseRestOfListLiteral(first: ListLiteralPart?): ListLiteral {
    val parts = ArrayList<ListLiteralPart>()
    first?.let { parts += it }

    while (true) {
        parts += when {
            consume(RBRACK) != null ->
                return ListLiteral(parts)

            consume(STAR) != null ->
                ListLiteralPart(parseExpression(), isSpread = true)

            else ->
                ListLiteralPart(parseExpression(), isSpread = false)
        }

        consume(COMMA)
    }
}

fun Token.unescapeString(): String {
    val raw = rawText

    if (raw.indexOf('\\') < 0)
        return raw.substring(1, raw.length - 1) // (strip quotes)

    syntaxError("Escape sequences not supported (yet)", pos) // TODO
}