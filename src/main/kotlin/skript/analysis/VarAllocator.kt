package skript.analysis

import skript.ast.*
import skript.io.ModuleName
import skript.syntaxError
import skript.util.Stack
import skript.util.isNotEmpty
import skript.withTop
import kotlin.math.max

class VarAllocator(val moduleName: ModuleName) : StatementVisitor, ExprVisitor {
    val scopeStack = Stack<Scope>()

    fun visitModule(content: List<Statement>): FunctionScope {
        val moduleScope = FunctionScope(null, false)

        scopeStack.push(moduleScope)
        try {
            visitBlock(Statements(content))
        } finally {
            scopeStack.pop()
        }

        return moduleScope
    }

    override fun visitBlock(stmts: Statements) {
        val parent = scopeStack.top()
        val top = parent.topScope()
        val varsHere = HashMap<String, VarInfo>()

        for (stmt in stmts.parts) {
            when (stmt) {
                is DeclareFunction -> {
                    val name = stmt.funcName ?: throw IllegalStateException("A stand-alone function declaration must have a name") // this shouldn't have parsed...

                    if (varsHere.containsKey(name))
                        syntaxError("$name is already defined", stmt.pos)

                    top.allocate(name).also {
                        varsHere[name] = it
                        stmt.hoistedVarInfo = it
                    }
                }

                is LetStatement -> {
                    for (decl in stmt.decls) {
                        val name = decl.varName
                        if (varsHere.containsKey(name))
                            syntaxError("$name is already defined", decl.pos)

                        top.allocate(name).also {
                            varsHere[name] = it
                            decl.varInfo = it
                        }
                    }
                }

                is ImportStatement -> {
                    for (decl in stmt.imports) {
                        val name = decl.importedName
                        if (varsHere.containsKey(name))
                            syntaxError("$name is already defined", decl.pos)

                        top.allocate(name).also {
                            varsHere[name] = it
                            decl.varInfo = it
                        }
                    }
                }

                else -> Unit
            }
        }

        scopeStack.withTop(BlockScope(parent, varsHere)) {
            for (stmt in stmts.parts)
                stmt.accept(this)
        }
    }

    override fun visitDeclareFunctionStmt(stmt: DeclareFunction) {
        visitDeclareFunction(stmt, true)
    }

    fun visitDeclareFunction(stmt: DeclareFunction, isStatement: Boolean) {
        val parent = scopeStack.top()

        val funcScope = FunctionScope(parent, stmt.implicitCtxLookup)
        stmt.innerFunScope = funcScope

        scopeStack.withTop(funcScope) {
            val varsHere = HashMap<String, VarInfo>()

            for (varDecl in stmt.params) {
                funcScope.allocate(varDecl.paramName).also {
                    varDecl.varInfo = it
                    varsHere[varDecl.paramName] = it
                }
            }

            scopeStack.withTop(BlockScope(funcScope, varsHere)) {
                stmt.body.accept(this)
            }
        }

        if (isStatement)
            check(stmt.isHoistedVarInfoDefined())
    }

    override fun visitFunctionLiteral(expr: FunctionLiteral) {
        visitDeclareFunction(expr.funDecl, false)
    }

    override fun visitLet(stmt: LetStatement) {
        for (decl in stmt.decls) {
            check(decl.isVarInfoDefined())
            decl.initializer?.accept(this)
        }
    }

    override fun visitImportStatement(stmt: ImportStatement) {
        for (decl in stmt.imports) {
            check(decl.isVarInfoDefined())
        }
    }

    override fun visitExportStatement(stmt: ExportStatement) {
        for (decl in stmt.exports) {
            decl.source.accept(this)
        }
    }

    override fun visitExprStmt(stmt: ExpressionStatement) {
        stmt.expression.accept(this)
    }

    override fun visitIf(stmt: IfStatement) {
        stmt.condition.accept(this)
        stmt.ifTrue.accept(this)
        stmt.ifFalse?.accept(this)
    }

    override fun visitWhile(stmt: WhileStatement) {
        stmt.condition.accept(this)
        stmt.body.accept(this)
    }

    override fun visitDoWhile(stmt: DoWhileStatement) {
        stmt.body.accept(this)
        stmt.condition.accept(this)
    }

    override fun visitForStatement(stmt: ForStatement) {
        stmt.container.accept(this)

        val parent = scopeStack.top()
        val top = parent.topScope()
        val varsHere = HashMap<String, VarInfo>()

        for (decl in stmt.decls) {
            val name = decl.varName

            top.allocate(name).also {
                varsHere[name] = it
                decl.varInfo = it
            }
        }

        scopeStack.withTop(BlockScope(parent, varsHere)) {
            stmt.body.accept(this)
        }
    }

    override fun visitBreakStatement(stmt: BreakStatement) {
        // nothing to do..
    }

    override fun visitContinueStatement(stmt: ContinueStatement) {
        // nothing to do..
    }

    override fun visitReturnStatement(stmt: ReturnStatement) {
        stmt.value?.accept(this)
    }

    override fun visitVarRef(expr: Variable) {
        var scope = scopeStack.top()
        var closureDepth = 0
        val funcs = Stack<FunctionScope>()

        while (true) {
            val varInfo = scope.getOwnVar(expr.varName)
            if (varInfo != null) {
                if (closureDepth == 0) {
                    expr.varInfo = varInfo
                } else {
                    expr.varInfo = ClosureVarInfo(varInfo as LocalVarInfo, closureDepth - 1)
                }
                break
            }

            when {
                scope is FunctionScope && scope.parent != null -> {
                    closureDepth++
                    funcs.push(scope)
                    scope = scope.parent!!
                }

                else -> {
                    scope = scope.parent ?: run {
                        if (expr.varName != "ctx" && funcs.any { it.implicitCtxLookup }) {
                            val ctx = Variable("ctx", expr.pos)
                            visitVarRef(ctx)
                            if (ctx.varInfo.location != VarLocation.GLOBAL) {
                                expr.varInfo = ImplicitCtxVarInfo(ctx.varInfo, expr.varName)
                                return
                            }
                        }

                        expr.varInfo = GlobalVarInfo(expr.varName)
                        return
                    }
                }
            }
        }

        var minDepth = 0
        while (funcs.isNotEmpty()) {
            val funcScope = funcs.pop()
            funcScope.closureDepthNeeded = max(funcScope.closureDepthNeeded, ++minDepth)
        }
    }

    override fun visitExtendsStatement(stmt: ExtendsStatement) {
        stmt.rewritten.accept(this)
    }

    override fun visitIncludeStatement(stmt: IncludeStatement) {
//        stmt.ctxParams?.accept(this)
        stmt.rewritten.accept(this)
    }

    override fun visitTemplateBlockStatement(stmt: TemplateBlockStatement) {
//        stmt.ctxParams?.accept(this)
//        stmt.content?.accept(this)
        stmt.rewritten.accept(this)
    }
}