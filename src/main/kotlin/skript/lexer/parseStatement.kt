package skript.lexer

import skript.ast.*
import skript.exec.ParamType
import skript.lexer.TokenType.*
import skript.syntaxError

fun Tokens.parseModule(moduleName: String): Module {
    return Module(moduleName, parseStatements(EOF, allowFunctions = true, allowClasses = true, allowVars = true))
}

fun Tokens.parseStatements(endingToken: TokenType, allowFunctions: Boolean = false, allowClasses: Boolean = false, allowVars: Boolean = false): List<Statement> {
    val result = ArrayList<Statement>()

    while (peek().type != endingToken)
        result += parseStatement(allowFunctions, allowClasses, allowVars)

    consume(endingToken).also { assert (it != null) }

    return result
}

fun Tokens.parseStatement(allowFunctions: Boolean, allowClasses: Boolean, allowVars: Boolean): Statement {
    return when (peek().type) {
        VAR,
        VAL -> {
            if (!allowVars)
                syntaxError("Variable declarations not allowed here", peek().pos)
            parseVarStatement()
        }

        FUNCTION -> {
            if (!allowFunctions)
                syntaxError("Function declarations not allowed here", peek().pos)
            parseFunctionDecl()
        }

        IF -> parseIfStatement()
        WHILE -> parseWhileStatement()
        FOR -> parseForStatement()
        DO -> parseDoWhileStatement()

        BREAK -> parseBreakStmt()
        CONTINUE -> parseContinueStmt()
        RETURN -> parseReturnStmt()
        SEMI -> EmptyStatement


        WHEN -> syntaxError("when is not implemented yet", peek().pos)
        TRY -> syntaxError("try is not implemented yet", peek().pos)
        THROW -> syntaxError("throw is not implemented yet", peek().pos)
        AT -> syntaxError("Annotations are not implemented yet", peek().pos)
        CLASS -> syntaxError("Classes are not implemented yet", peek().pos)

        EOF -> {
            syntaxError("Unexpected end of file", peek().pos)
        }

        else -> ExpressionStatement(parseExpression())
    }
}

enum class AllowInit {
    OPTIONAL,
    REQUIRED,
    FORBIDDEN
}

internal fun Tokens.parseReturnStmt(): ReturnStatement {
    next().also { assert(it.type == RETURN) }

    val value = if (peek().type != SEMI) {
        parseExpression()
    } else {
        null
    }

    expect(SEMI)

    return ReturnStatement(value)
}

internal fun Tokens.parseParamDecls(): List<ParamDecl> {
    var varArgState = 0 // 0 = regular params, 1 = after *posArgs, 2 = after **kwArgs

    val params = ArrayList<ParamDecl>()
    val namesSeen = HashSet<String>()
    while (true) {
        expect(if (params.isEmpty()) LPAREN else COMMA)

        val paramType = when (peek().type) {
            STAR_STAR -> {
                if (varArgState >= 2)
                    syntaxError("There can only be one **kwArgs and it must be last", peek().pos)
                varArgState = 2
                next()
                ParamType.KW_ARGS
            }
            STAR -> {
                if (varArgState >= 1)
                    syntaxError("There can only be one *posArgs, and it must be after all regular paremeters and before **kwArgs", peek().pos)
                varArgState = 1
                next()
                ParamType.POS_ARGS
            }
            else -> {
                if (varArgState > 0)
                    syntaxError("Regular parameters must all be before *posArgs and **kwArgs", peek().pos)

                ParamType.NORMAL
            }
        }

        val name = expect(IDENTIFIER)
        if (!namesSeen.add(name.rawText))
            syntaxError("Can't use the same parameter name twice", name.pos)

        val defaultValue = if (consume(ASSIGN) != null) {
            parseExpression()
        } else {
            null
        }

        params.add(ParamDecl(name.rawText, paramType, defaultValue))

        if (consume(RPAREN) != null)
            break;
    }

    return params
}

internal fun Tokens.parseFunctionDecl(): DeclareFunction {
    next().also { assert(it.type == FUNCTION) }

    val name = expect(IDENTIFIER)
    val params = parseParamDecls()

    expect(LCURLY)
    val body = parseStatements(RCURLY, allowFunctions = true, allowClasses = false, allowVars = true)

    return DeclareFunction(name.rawText, params, Statements(body))
}

internal fun Tokens.parseBreakStmt(): BreakStatement {
    val breakTok = next().also { assert(it.type == BREAK) }
    val identOpt = consume(IDENTIFIER)
    expect(SEMI)
    return BreakStatement(identOpt?.rawText, identOpt?.pos ?: breakTok.pos)
}

internal fun Tokens.parseContinueStmt(): ContinueStatement {
    val contTok = next().also { assert(it.type == CONTINUE) }
    val identOpt = consume(IDENTIFIER)
    expect(SEMI)
    return ContinueStatement(identOpt?.rawText, identOpt?.pos ?: contTok.pos)
}

internal fun Tokens.parseVarDecls(endingToken: TokenType, allowInit: AllowInit): List<VarDecl> {
    val decls = ArrayList<VarDecl>()

    while (true) {
        decls += parseVarDecl(allowInit)

        if (consume(endingToken) != null) {
            break
        } else {
            expect(COMMA)
        }
    }

    val namesSeen = HashSet<String>()
    for (decl in decls)
        if (!namesSeen.add(decl.varName))
            syntaxError("Can't use the same name twice", decl.pos)

    return decls
}

internal fun Tokens.parseVarDecl(allowInit: AllowInit): VarDecl {
    val ident = expect(IDENTIFIER)

    val assignOpt = consume(ASSIGN)
    return if (assignOpt != null) {
        if (allowInit == AllowInit.FORBIDDEN)
            syntaxError("Can't initialize a variable here", assignOpt.pos)

        VarDecl(ident.rawText, parseExpression(), ident.pos)
    } else {
        if (allowInit == AllowInit.REQUIRED)
            syntaxError("This variable must be initialized", ident.pos)

        VarDecl(ident.rawText, null, ident.pos)
    }
}

internal fun Tokens.parseVarStatement(): LetStatement {
    val varTok = next().also { assert(it.type == VAR || it.type == VAL) }

    // TODO: relax this once we can determine the variable is initialized exactly once before being used?
    val allowInit = if (varTok.type == VAL) AllowInit.REQUIRED else AllowInit.OPTIONAL

    val decls = parseVarDecls(SEMI, allowInit)

    return LetStatement(decls)
}

internal fun Tokens.parseIfStatement(): IfStatement {
    next().also { assert(it.type == IF) }

    expect(LPAREN)
    val condition = parseExpression()
    expect(RPAREN)

    val ifTrue = parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)

    val ifFalse = if (consume(ELSE) != null) {
        parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)
    } else {
        null
    }

    return IfStatement(condition, ifTrue, ifFalse)
}

internal fun Tokens.parseWhileStatement(): WhileStatement {
    next().also { assert(it.type == WHILE) }

    expect(LPAREN)
    val condition = parseExpression()
    expect(RPAREN)

    val body = parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)

    return WhileStatement(condition, body)
}

internal fun Tokens.parseDoWhileStatement(): DoWhileStatement {
    next().also { assert(it.type == DO) }

    expect(LCURLY)
    val body = Statements(parseStatements(RCURLY, allowFunctions = true, allowClasses = false, allowVars = true))

    expect(WHILE)
    expect(LPAREN)
    val condition = parseExpression()
    expect(RPAREN)

    consume(SEMI)

    return DoWhileStatement(body, condition)
}

internal fun Tokens.parseForStatement(): ForStatement {
    next().also { assert(it.type == FOR) }

    expect(LPAREN)
    val openTuple = consume(LPAREN)
    val decls = if (openTuple != null) {
        parseVarDecls(RPAREN, AllowInit.FORBIDDEN).also {
            if (it.size !in 1..2) {
                syntaxError("With a for loop, only one or two iteration variables can be used", openTuple.pos)
            }
        }
    } else {
        listOf(parseVarDecl(AllowInit.FORBIDDEN))
    }

    expect(IN)
    val container = parseExpression()
    expect(RPAREN)

    val body = parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)

    return ForStatement(decls, container, body)
}

internal fun Tokens.parseStatementOrBlock(allowFunctions: Boolean, allowClasses: Boolean, allowVars: Boolean): Statement {
    return if (consume(LCURLY) != null) {
        Statements(parseStatements(RCURLY, allowFunctions, allowClasses, allowVars))
    } else {
        parseStatement(allowFunctions, allowClasses, allowVars)
    }
}