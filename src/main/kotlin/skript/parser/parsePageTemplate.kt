package skript.parser

import skript.ast.*
import skript.parser.TokenType.*
import skript.syntaxError
import skript.values.SkString

class PageTemplateParser(tokens: Tokens) : ExpressionParser(tokens) {
    fun parsePageTemplate(moduleName: String): ParsedModule {
        return ParsedModule(moduleName, parseBlockUntilEnd(null))
    }

    private fun processEmitFilters(expr: Expression, templateRuntime: Expression): Expression {
        if (expr !is PipeCall)
            return expr

        val input = processEmitFilters(expr.input, templateRuntime)

        val target = when (expr.target) {
            is Variable -> { // input |> foo  =>  input |> templateRuntime.filters.foo()
                PropertyAccess(PropertyAccess(templateRuntime, "filters"), expr.target.varName)
            }
            is FuncCall -> {
                if (expr.target.func is Variable) { // input |> foo(...)   =>   input |> templateRuntime.filters.foo(...)
                    FuncCall(PropertyAccess(PropertyAccess(templateRuntime, "filters"), expr.target.func.varName), expr.target.args)
                } else {
                    expr.target
                }
            }
            else -> expr.target
        }

        if (input != expr.input || target != expr.target)
            return PipeCall(input, target)

        return expr
    }

    fun buildEmit(expr: Expression, escapes: List<Expression>?, pos: Pos): Statement {
        val templateRuntime = Variable("templateRuntime", pos)

        val value = processEmitFilters(expr, templateRuntime)

        val emitArg = when {
            escapes == null -> value // raw template text

            escapes.isEmpty() -> // inout  =>  input |> templateRuntime.defaultEscape()
                PipeCall(value, FuncCall(PropertyAccess(templateRuntime, "defaultEscape"), emptyList()))

            else -> {
                var curr = value
                for (escape in escapes) {
                    val target = when (escape) {
                        is Variable -> { // input -> foo  =>  input |> templateRuntime.escapes["foo"]()
                            FuncCall(PropertyAccess(PropertyAccess(templateRuntime, "escapes"), escape.varName), emptyList())
                        }
                        is FuncCall -> {
                            if (escape.func is Variable) { // input -> foo(...)   =>   input |> templateRuntime.escapes["foo"](...)
                                FuncCall(PropertyAccess(PropertyAccess(templateRuntime, "escapes"), escape.func.varName), escape.args)
                            } else {
                                escape
                            }
                        }
                        else -> escape
                    }
                    curr = PipeCall(curr, target)
                }
                curr
            }
        }

        return ExpressionStatement(MethodCall(templateRuntime, "emit", listOf(PosArg(emitArg)), MethodCallType.REGULAR))
    }


    internal fun parseBlockUntilEnd(endTokens: Set<String>?): List<Statement> {
        val result = ArrayList<Statement>()

        while (tokens.hasMore()) {
            val tok = tokens.next()

            when (tok.type) {
                ECHO_NL, ECHO_TEXT, ECHO_WS -> {
                    val str = Literal(SkString(tok.rawText))
                    result += buildEmit(str, null, tok.pos)
                }

                EXPR_OPEN -> {
                    result += parseRestOfExpression(tok.pos)
                    tokens.expect(EXPR_CLOSE)
                }

                STMT_OPEN -> {
                    if (endTokens != null && tokens.peek().rawText in endTokens)
                        return result

                    result += parseRestOfStatement()
                    tokens.expect(STMT_CLOSE)
                }

                EOF -> {
                    if (endTokens == null) {
                        return result
                    } else {
                        syntaxError("Unexpected end-of-file", tok.pos)
                    }
                }

                else -> syntaxError("Unexpected " + tok.rawText, tok.pos)
            }
        }

        syntaxError("Unexpected end of tokens")
    }


    fun parseRestOfExpression(pos: Pos): Statement {
        val value = parseExpression()

        if (peekType == ARROW) {
            val escapes = ArrayList<Expression>()
            while (tokens.consume(ARROW) != null) {
                escapes += parseExpression()
            }
            return buildEmit(value, escapes, pos)
        } else {
            return buildEmit(value, emptyList(), pos)
        }
    }

    fun parseRestOfStatement(): Statement {
        return when (peekType) {
            VAR,
            VAL -> {
                parseVarStatement()
            }

            IF -> parseIfStatement()
            FOR -> parseForStatement()

            BREAK -> parseBreakStmt()
            CONTINUE -> parseContinueStmt()

            EOF -> {
                syntaxError("Unexpected end of file", peekPos)
            }

            else -> {
                val expr = parseExpression()
                return ExpressionStatement(expr)
            }
        }
    }

    internal fun parseBreakStmt(): BreakStatement {
        val breakTok = tokens.expect(BREAK)
        val identOpt = if (tokens.consume(AT) != null) tokens.expect(IDENTIFIER) else null
        return BreakStatement(identOpt?.rawText, identOpt?.pos ?: breakTok.pos)
    }

    internal fun parseContinueStmt(): ContinueStatement {
        val contTok = tokens.expect(CONTINUE)
        val identOpt = if (tokens.consume(AT) != null) tokens.expect(IDENTIFIER) else null
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

        val decls = parseVarDecls(allowInit) { peekType == STMT_CLOSE }

        return LetStatement(decls, false)
    }

    internal fun parseIfStatement(): IfStatement {
        tokens.expect(IF)

        return parseRestOfIfStatement()
    }

    private fun parseRestOfIfStatement(): IfStatement {
        val condition = parseExpression()
        tokens.expect(STMT_CLOSE)

        val ifTrue = parseBlockUntilEnd(setOf("else", "elif", "end")).pack()

        val ifFalse = when {
            tokens.consume(ELSE) != null -> {
                tokens.expect(STMT_CLOSE)
                val stmts = parseBlockUntilEnd(setOf("end"))
                tokens.expect(IDENTIFIER)
                tokens.consume(IF) // optional
                stmts.pack()
            }

            tokens.consume(ELIF) != null -> {
                parseRestOfIfStatement()
            }

            else -> {
                tokens.expect(IDENTIFIER) // end
                tokens.consume(IF) // optional
                null
            }
        }

        return IfStatement(condition, ifTrue, ifFalse)
    }


    internal fun parseForStatement(stmtLabel: String? = null): ForStatement {
        tokens.expect(FOR)

        val firstVar = parseVarDecl(AllowInit.FORBIDDEN)
        val secondVar = if (tokens.consume(COMMA) != null) {
            parseVarDecl(AllowInit.FORBIDDEN)
        } else {
            null
        }

        tokens.expect(IN)
        val container = parseExpression()
        tokens.expect(STMT_CLOSE)

        val body = parseBlockUntilEnd(setOf("end"))

        tokens.expect(IDENTIFIER) // end
        tokens.consume(FOR)

        return ForStatement(listOfNotNull(firstVar, secondVar), container, body.pack(), stmtLabel)
    }
}

fun List<Statement>.pack(): Statement {
    return singleOrNull() ?: Statements(this)
}