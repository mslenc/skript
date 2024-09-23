package skript.parser

import skript.ast.*
import skript.exec.ParamType
import skript.parser.TokenType.*
import skript.syntaxError

class ModuleParser(tokens: Tokens) : ExpressionParser(tokens) {
    fun parseModule(moduleName: String): Module {
        return Module(moduleName, parseStatements(EOF, allowFunctions = true, allowClasses = true, allowVars = true))
    }

    override fun parsePrimary(): Expression {
        if (peekType == FUNCTION) {
            consume()
            return FunctionLiteral(parseFunctionDecl(funLiteral = true))
        }

        return super.parsePrimary()
    }

    fun parseStatements(endingToken: TokenType, allowFunctions: Boolean = false, allowClasses: Boolean = false, allowVars: Boolean = false): List<Statement> {
        val result = ArrayList<Statement>()

        while (peekType != endingToken)
            result += parseStatement(allowFunctions, allowClasses, allowVars)

        tokens.consume(endingToken).also { assert (it != null) }

        return result
    }

    fun parseStatement(allowFunctions: Boolean, allowClasses: Boolean, allowVars: Boolean): Statement {
        val stmtLabel = if (peekType == IDENTIFIER) {
            val ident = tokens.next()
            if (tokens.consume(AT) != null) {
                ident.rawText
            } else {
                tokens.putBack(ident)
                null
            }
        } else {
            null
        }

        if (stmtLabel != null) {
            return when (peekType) {
                WHILE -> parseWhileStatement(stmtLabel)
                FOR -> parseForStatement(stmtLabel)
                DO -> parseDoWhileStatement(stmtLabel)
                else -> syntaxError("After a label@, expected a while, for or do loop")
            }
        }

        return when (peekType) {
            VAR,
            VAL -> {
                if (!allowVars)
                    syntaxError("Variable declarations not allowed here", peekPos)
                parseVarStatement()
            }

            FUNCTION -> {
                if (!allowFunctions)
                    syntaxError("Function declarations not allowed here", peekPos)
                parseFunctionDecl(funLiteral = false)
            }

            IF -> parseIfStatement()
            WHILE -> parseWhileStatement()
            FOR -> parseForStatement()
            DO -> parseDoWhileStatement()

            BREAK -> parseBreakStmt()
            CONTINUE -> parseContinueStmt()
            RETURN -> parseReturnStmt()
            SEMI -> EmptyStatement


            WHEN -> syntaxError("when is not implemented yet", peekPos)
            TRY -> syntaxError("try is not implemented yet", peekPos)
            THROW -> syntaxError("throw is not implemented yet", peekPos)
            AT -> syntaxError("Annotations are not implemented yet", peekPos)
            CLASS -> syntaxError("Classes are not implemented yet", peekPos)

            EOF -> {
                syntaxError("Unexpected end of file", peekPos)
            }

            else -> {
                val expr = parseExpression()
                tokens.expectEndOfStatement()
                ExpressionStatement(expr)
            }
        }
    }

    internal fun parseReturnStmt(): ReturnStatement {
        tokens.expect(RETURN)

        val value = if (tokens.atEndOfStatement()) {
            null
        } else {
            parseExpression()
        }

        tokens.expectEndOfStatement()

        return ReturnStatement(value)
    }

    internal fun parseParamDecls(): List<ParamDecl> {
        var varArgState = 0 // 0 = regular params, 1 = after *posArgs, 2 = after **kwArgs

        val params = ArrayList<ParamDecl>()
        val namesSeen = HashSet<String>()

        tokens.expect(LPAREN)

        while (true) {
            if (tokens.consume(RPAREN) != null)
                break

            val paramType = when (peekType) {
                STAR_STAR -> {
                    if (varArgState >= 2)
                        syntaxError("There can only be one **kwArgs and it must be last", peekPos)
                    varArgState = 2
                    consume()
                    ParamType.KW_ARGS
                }
                STAR -> {
                    if (varArgState >= 1)
                        syntaxError("There can only be one *posArgs, and it must be after all regular paremeters and before **kwArgs", peekPos)
                    varArgState = 1
                    consume()
                    ParamType.POS_ARGS
                }
                else -> {
                    if (varArgState > 0)
                        syntaxError("Regular parameters must all be before *posArgs and **kwArgs", peekPos)

                    ParamType.NORMAL
                }
            }

            val name = tokens.expect(IDENTIFIER)
            if (!namesSeen.add(name.rawText))
                syntaxError("Can't use the same parameter name twice", name.pos)

            val defaultValue = if (tokens.consume(ASSIGN) != null) {
                parseExpression()
            } else {
                null
            }

            params.add(ParamDecl(name.rawText, paramType, defaultValue))

            if (peekType != RPAREN)
                tokens.expect(COMMA)
        }

        return params
    }

    internal fun parseFunctionDecl(funLiteral: Boolean): DeclareFunction {
        if (!funLiteral)
            tokens.expect(FUNCTION)

        val pos = peekPos

        val name = when {
            !funLiteral -> tokens.expect(IDENTIFIER)
            peekType == IDENTIFIER -> consume()
            else -> null
        }

        val params = parseParamDecls()

        tokens.expect(LCURLY)
        val body = parseStatements(RCURLY, allowFunctions = true, allowClasses = false, allowVars = true)

        return DeclareFunction(name?.rawText, params, Statements(body), pos)
    }

    internal fun parseBreakStmt(): BreakStatement {
        val breakTok = tokens.expect(BREAK)
        val identOpt = if (tokens.consume(AT) != null) tokens.expect(IDENTIFIER) else null
        tokens.expectEndOfStatement()
        return BreakStatement(identOpt?.rawText, identOpt?.pos ?: breakTok.pos)
    }

    internal fun parseContinueStmt(): ContinueStatement {
        val contTok = tokens.expect(CONTINUE)
        val identOpt = if (tokens.consume(AT) != null) tokens.expect(IDENTIFIER) else null
        tokens.expectEndOfStatement()
        return ContinueStatement(identOpt?.rawText, identOpt?.pos ?: contTok.pos)
    }

    internal fun parseVarDecls(allowInit: AllowInit, endingCondition: ()->Boolean): List<VarDecl> {
        val decls = ArrayList<VarDecl>()

        while (true) {
            decls += parseVarDecl(allowInit)

            if (endingCondition()) {
                break
            } else {
                tokens.expect(COMMA)
            }
        }

        val namesSeen = HashSet<String>()
        for (decl in decls)
            if (!namesSeen.add(decl.varName))
                syntaxError("Can't use the same name twice", decl.pos)

        return decls
    }

    internal fun parseVarDecl(allowInit: AllowInit): VarDecl {
        val ident = tokens.expect(IDENTIFIER)

        val assignOpt = tokens.consume(ASSIGN)
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

    internal fun parseVarStatement(): LetStatement {
        val varTok = tokens.next().also { assert(it.type == VAR || it.type == VAL) }

        // TODO: relax this once we can determine the variable is initialized exactly once before being used?
        val allowInit = if (varTok.type == VAL) AllowInit.REQUIRED else AllowInit.OPTIONAL

        val decls = parseVarDecls(allowInit) { tokens.atEndOfStatement() }
        tokens.expectEndOfStatement()

        return LetStatement(decls)
    }

    internal fun parseIfStatement(): IfStatement {
        tokens.expect(IF)

        return parseRestOfIfStatement()
    }

    private fun parseRestOfIfStatement(): IfStatement {
        tokens.expect(LPAREN)
        val condition = parseExpression()
        tokens.expect(RPAREN)

        val ifTrue = parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)

        val ifFalse = when {
            tokens.consume(ELSE) != null -> parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)
            tokens.consume(ELIF) != null -> parseRestOfIfStatement()
            else -> null
        }

        return IfStatement(condition, ifTrue, ifFalse)
    }

    internal fun parseWhileStatement(stmtLabel: String? = null): WhileStatement {
        tokens.expect(WHILE)

        tokens.expect(LPAREN)
        val condition = parseExpression()
        tokens.expect(RPAREN)

        val body = parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)

        return WhileStatement(condition, body, stmtLabel)
    }

    internal fun parseDoWhileStatement(stmtLabel: String? = null): DoWhileStatement {
        tokens.expect(DO)

        tokens.expect(LCURLY)
        val body = Statements(parseStatements(RCURLY, allowFunctions = true, allowClasses = false, allowVars = true))

        tokens.expect(WHILE)
        tokens.expect(LPAREN)
        val condition = parseExpression()
        tokens.expect(RPAREN)

        tokens.expectEndOfStatement()

        return DoWhileStatement(body, condition, stmtLabel)
    }

    internal fun parseForStatement(stmtLabel: String? = null): ForStatement {
        tokens.expect(FOR)

        tokens.expect(LPAREN)
        val openTuple = tokens.consume(LPAREN)
        val decls = if (openTuple != null) {
            parseVarDecls(AllowInit.FORBIDDEN) { peekType == RPAREN }.also {
                tokens.consume(RPAREN) ?: throw IllegalStateException()
                if (it.size !in 1..2) {
                    syntaxError("With a for loop, only one or two iteration variables can be used", openTuple.pos)
                }
            }
        } else {
            listOf(parseVarDecl(AllowInit.FORBIDDEN))
        }

        tokens.expect(IN)
        val container = parseExpression()
        tokens.expect(RPAREN)

        val body = parseStatementOrBlock(allowFunctions = true, allowClasses = false, allowVars = true)

        return ForStatement(decls, container, body, stmtLabel)
    }

    internal fun parseStatementOrBlock(allowFunctions: Boolean, allowClasses: Boolean, allowVars: Boolean): Statement {
        return if (tokens.consume(LCURLY) != null) {
            Statements(parseStatements(RCURLY, allowFunctions, allowClasses, allowVars))
        } else {
            parseStatement(allowFunctions, allowClasses, allowVars)
        }
    }

    fun Tokens.atEndOfStatement(): Boolean {
        return when (peek().type) {
            SEMI,
            RCURLY,
            EOF -> true
            else -> peekOnNewLine()
        }
    }

    fun Tokens.expectEndOfStatement() {
        if (consume(SEMI) == null && !atEndOfStatement()) {
            syntaxError("Expected end of statement (a semi-colon ; or a curly brace } or a new line)", peek().pos)
        }
    }
}

enum class AllowInit {
    OPTIONAL,
    REQUIRED,
    FORBIDDEN
}