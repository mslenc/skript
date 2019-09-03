package skript.ast

import skript.analysis.LocalVarInfo
import skript.analysis.VarInfo
import skript.exec.ParamType
import skript.lexer.Pos
import skript.util.AstProps

interface StatementVisitor {
    fun visitBlock(stmts: Statements)
    fun visitIf(stmt: IfStatement)
    fun visitExprStmt(stmt: ExpressionStatement)
    fun visitWhile(stmt: WhileStatement)
    fun visitDoWhile(stmt: DoWhileStatement)
    fun visitForStatement(stmt: ForStatement)
    fun visitLet(stmt: LetStatement)
    fun visitDeclareFunctionStmt(stmt: DeclareFunction)
    fun visitReturnStatement(stmt: ReturnStatement)
    fun visitBreakStatement(stmt: BreakStatement)
    fun visitContinueStatement(stmt: ContinueStatement)
}

abstract class Statement {
    val props = AstProps()

    abstract fun accept(visitor: StatementVisitor)
}

object EmptyStatement : Statement() {
    override fun accept(visitor: StatementVisitor) {
        // we just ignore it..
    }
}

class BreakStatement(val label: String?, val pos: Pos) : Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitBreakStatement(this)
}

class ContinueStatement(val label: String?, val pos: Pos) : Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitContinueStatement(this)
}

class Statements(val parts: List<Statement>): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitBlock(this)
}

class IfStatement(val condition: Expression, val ifTrue: Statement, val ifFalse: Statement?): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitIf(this)
}

class ExpressionStatement(val expression: Expression): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitExprStmt(this)
}

class WhileStatement(val condition: Expression, val body: Statement): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitWhile(this)
}

class DoWhileStatement(val body: Statement, val condition: Expression): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitDoWhile(this)
}

class ForStatement(val decls: List<VarDecl>, val container: Expression, val body: Statement): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitForStatement(this)
}

class VarDecl(val varName: String, val initializer: Expression?, val pos: Pos) {
    lateinit var varInfo: VarInfo

    internal fun isVarInfoThere(): Boolean {
        return ::varInfo.isInitialized
    }
}

class LetStatement(val decls: List<VarDecl>): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitLet(this)
}

class ParamDecl(val paramName: String, val paramType: ParamType, val defaultValue: Expression?) {
    lateinit var varInfo: LocalVarInfo
}

class DeclareFunction(val funcName: String, val params: List<ParamDecl>, val body: Statements) : Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitDeclareFunctionStmt(this)
}

class ReturnStatement(val value: Expression?) : Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitReturnStatement(this)
}