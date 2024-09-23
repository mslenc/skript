package skript.parser

import skript.ast.*
import skript.parser.TokenType.*
import skript.syntaxError
import skript.values.*



open class ExpressionParser(val tokens: Tokens) {
    protected val peekType: TokenType
        get() = tokens.peek().type

    protected val peekPos: Pos
        get() = tokens.peek().pos

    protected fun consume() = tokens.next()

    protected fun consumePos() = consume().pos

    fun parseExpression(): Expression {
        return parseAssignment()
    }

    fun parseAssignment(): Expression {
        val left = parseTernary()

        if (left !is LValue)
            return left

        val nextTok = tokens.peek()

        val binaryOp = when {
            nextTok.type == ASSIGN -> null
            else -> nextTok.type.toAssignOp() ?: return left
        }
        tokens.next()

        val value = parseAssignment()

        return AssignExpression(left, binaryOp, value)
    }


    fun parseTernary(): Expression {
        val condition = parseDisjunction()

        return if (tokens.consume(QUESTION) != null) {
            val ifTrue = parseAssignment()
            tokens.expect(COLON)
            val ifFalse = parseAssignment()
            TernaryExpression(condition, ifTrue, ifFalse)
        } else {
            condition
        }
    }

    fun parseDisjunction(): Expression {
        var result = parseConjunction()
        while (true) {
            result = when (peekType) {
                OR ->    BinaryExpression(consumePos(), result, BinaryOp.OR,    parseConjunction())
                OR_OR -> BinaryExpression(consumePos(), result, BinaryOp.OR_OR, parseConjunction())
                else -> return result
            }
        }
    }

    fun parseConjunction(): Expression {
        var result = parseComparison()
        while (true) {
            result = when (peekType) {
                AND ->     BinaryExpression(consumePos(), result, BinaryOp.AND,     parseComparison())
                AND_AND -> BinaryExpression(consumePos(), result, BinaryOp.AND_AND, parseComparison())
                else -> return result
            }
        }
    }

    fun parseComparison(): Expression {
        // we avoid allocating the lists for 0 and 1 operator, because those are expected to be the vast majority, and also
        // to allow STARSHIP
        // (but we do need the lists in bigger cases, because we only allow sequences where all operators are compatible, listed above)

        val first = parseInIs()
        val pos = peekPos
        val firstOp = peekType.toComparisonOp()?.also { tokens.next() } ?: return first
        val second = parseInIs()
        val secondOp = peekType.toComparisonOp()?.also { tokens.next() } ?: return BinaryExpression(pos, first, firstOp, second)

        val parts = mutableListOf(first, second, parseInIs())
        val ops = mutableListOf(firstOp, secondOp)

        while (true) {
            ops += peekType.toComparisonOp()?.also { tokens.next() } ?: break
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

    fun parseInIs(): Expression {
        var result = parseElvis()

        while (true) {
            result = when (peekType) {
                IN ->     { tokens.next(); ValueIn(result, parseElvis(), true) }
                NOT_IN -> { tokens.next(); ValueIn(result, parseElvis(), false) }

                IS ->     { tokens.next(); val ident = tokens.expect(IDENTIFIER); ObjectIs(result, Variable(ident.rawText, ident.pos), true) }
                NOT_IS -> { tokens.next(); val ident = tokens.expect(IDENTIFIER); ObjectIs(result, Variable(ident.rawText, ident.pos), false) }

                else -> return result
            }
        }
    }

    fun parseElvis(): Expression {
        var result = parseInfixCall()
        while (true) {
            result = when (peekType) {
                ELVIS -> BinaryExpression(tokens.next().pos, result, BinaryOp.ELVIS, parseInfixCall())
                else -> return result
            }
        }
    }

    fun parseInfixCall(): Expression {
        var result = parseRange()
        while (true) {
            result = when {
                peekType == PIPE_CALL -> {
                    tokens.next()
                    val target = parseRange()

                    PipeCall(result, target)
                    /*
                    val arg = listOf(PosArg(result))

                    when (target) {
                        is FuncCall -> FuncCall(target.func, arg + target.args)
                        is MethodCall -> MethodCall(target.obj, target.methodName, arg + target.args, target.type)
                        else -> FuncCall(target, arg)
                    }

                     */
                }
                tokens.peekOnNewLine() || peekType != IDENTIFIER -> return result
                else -> MethodCall(result, tokens.next().rawText, listOf(PosArg(parseRange())), MethodCallType.INFIX)
            }
        }
    }

    fun parseRange(): Expression {
        var result = parseAdd()
        while (true) {
            result = when (peekType) {
                DOT_DOT ->      BinaryExpression(tokens.next().pos, result, BinaryOp.RANGE_TO,      parseAdd())
                DOT_DOT_LESS -> BinaryExpression(tokens.next().pos, result, BinaryOp.RANGE_TO_EXCL, parseAdd())
                else -> return result
            }
        }
    }

    fun parseAdd(): Expression {
        var result = parseMul()
        while (true) {
            result = when (peekType) {
                PLUS ->  BinaryExpression(tokens.next().pos, result, BinaryOp.ADD,      parseMul())
                MINUS -> BinaryExpression(tokens.next().pos, result, BinaryOp.SUBTRACT, parseMul())
                else -> return result
            }
        }
    }

    fun parseMul(): Expression {
        var result = parsePower()
        while (true) {
            result = when (peekType) {
                STAR ->        BinaryExpression(tokens.next().pos, result, BinaryOp.MULTIPLY,   parsePower())
                SLASH ->       BinaryExpression(tokens.next().pos, result, BinaryOp.DIVIDE,     parsePower())
                SLASH_SLASH -> BinaryExpression(tokens.next().pos, result, BinaryOp.DIVIDE_INT, parsePower())
                PERCENT ->     BinaryExpression(tokens.next().pos, result, BinaryOp.REMAINDER,  parsePower())
                else -> return result
            }
        }
    }

    fun parsePower(): Expression {
        var result = parsePrefixUnary()
        while (true) {
            result = when (peekType) {
                STAR_STAR -> BinaryExpression(tokens.next().pos, result, BinaryOp.POWER, parsePower())
                else -> return result
            }
        }
    }

    fun parsePrefixUnary(): Expression {
        return when (peekType) {
            MINUS -> { tokens.next(); UnaryExpression(UnaryOp.MINUS, parsePrefixUnary()) }
            PLUS -> { tokens.next(); UnaryExpression(UnaryOp.PLUS, parsePrefixUnary()) }
            EXCL -> { tokens.next(); UnaryExpression(UnaryOp.NOT, parsePrefixUnary()) }
            PLUS_PLUS -> {
                val tok = tokens.next()
                val inner = parsePrefixUnary()
                if (inner is LValue) {
                    PrePostExpr(PrePostOp.PRE_INCR, inner)
                } else {
                    syntaxError("Pre-increment ++ can only be used on lvalues", tok.pos)
                }
            }
            MINUS_MINUS -> {
                val tok = tokens.next()
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

    fun parsePostfixUnary(): Expression {
        return parsePostfixes(parsePrimary())
    }

    fun parsePostfixes(primary: Expression): Expression {
        var result = primary

        while (true) {
            if (tokens.peekOnNewLine() && peekType != SAFE_DOT && peekType != DOT) // all others can start a new statement
                return result

            result = when (peekType) {
                PLUS_PLUS -> {
                    val tok = tokens.next()
                    if (result is LValue) {
                        PrePostExpr(PrePostOp.POST_INCR, result)
                    } else {
                        syntaxError("Post-increment ++ can only be used on lvalues", tok.pos)
                    }
                }
                MINUS_MINUS -> {
                    val tok = tokens.next()
                    if (result is LValue) {
                        PrePostExpr(PrePostOp.POST_DECR, result)
                    } else {
                        syntaxError("Post-decrement -- can only be used on lvalues", tok.pos)
                    }
                }
                SAFE_DOT,
                DOT -> {
                    val callType = if (tokens.next().type == SAFE_DOT) MethodCallType.SAFE else MethodCallType.REGULAR
                    val ident = tokens.expect(IDENTIFIER)
                    if (peekType == LPAREN) {
                        MethodCall(result, ident.rawText, parseCallArgs(), callType)
                    } else {
                        PropertyAccess(result, ident.rawText)
                    }
                }
                LBRACK -> {
                    tokens.next()
                    val index = parseExpression()
                    tokens.expect(RBRACK)
                    ElementAccess(result, index)
                }
                LPAREN -> {
                    FuncCall(result, parseCallArgs())
                }
                else -> return result
            }
        }
    }

    fun parseCallArgs(): List<CallArg> {
        val args = ArrayList<CallArg>()
        val namesSeen = HashSet<String>()

        var kwOnly = false

        tokens.expect(LPAREN)
        while (true) {
            val peeked = tokens.peek()
            when (peeked.type) {
                RPAREN -> {
                    tokens.next()
                    return args
                }
                STAR -> {
                    if (kwOnly) {
                        syntaxError("Positional arguments must come before keyword arguments", peeked.pos)
                    } else {
                        tokens.next()
                        args += SpreadPosArg(parseExpression())
                    }
                }
                STAR_STAR -> {
                    tokens.next()
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

            if (peekType != RPAREN) {
                tokens.expect(COMMA)
            }
        }
    }

    open fun parsePrimary(): Expression {
        if (peekType == EOF)
            syntaxError("Expected an expression, not <EOF>", peekPos)

        val tok = tokens.next()

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
                tokens.expect(RPAREN)
                return Parentheses(result)
            }

            IDENTIFIER -> {
                return Variable(tok.rawText, tok.pos)
            }

            LBRACK -> {
                return parseRestOfListLiteral()
            }

            LCURLY -> {
                return parseRestOfMapLiteral()
            }

            STRING_TEMPLATE -> {
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
                    val sub = ExpressionParser(Tokens(part.tokens))
                    val expr = sub.parseExpression()
                    if (sub.peekType != EOF) {
                        syntaxError("Unexpected token", sub.peekPos)
                    } else {
                        StrTemplateExpr(expr)
                    }
                }
            }
        }

        return StringTemplateExpr(parts)
    }

    fun parseRestOfMapLiteral(): MapLiteral {
        val parts = ArrayList<MapLiteralPart>()

        while (true) {
            parts += when {
                tokens.consume(RCURLY) != null -> {
                    return MapLiteral(parts)
                }

                tokens.consume(STAR_STAR) != null -> {
                    val spread = parseExpression()
                    MapLiteralPartSpread(spread)
                }

                tokens.consume(LBRACK) != null -> {
                    val key = parseExpression()
                    tokens.expect(RBRACK)
                    tokens.expect(COLON)
                    val value = parseExpression()
                    MapLiteralPartExprKey(key, value)
                }

                else -> {
                    val ident = tokens.expect(IDENTIFIER)
                    tokens.expect(COLON)
                    val value = parseExpression()
                    MapLiteralPartFixedKey(ident.rawText, value)
                }
            }

            if (peekType != RCURLY)
                tokens.expect(COMMA)
        }
    }

    fun parseRestOfListLiteral(): ListLiteral {
        val parts = ArrayList<ListLiteralPart>()

        while (true) {
            parts += when {
                tokens.consume(RBRACK) != null ->
                    return ListLiteral(parts)

                tokens.consume(STAR) != null ->
                    ListLiteralPart(parseExpression(), isSpread = true)

                else ->
                    ListLiteralPart(parseExpression(), isSpread = false)
            }

            if (peekType != RBRACK)
                tokens.expect(COMMA)
        }
    }
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
