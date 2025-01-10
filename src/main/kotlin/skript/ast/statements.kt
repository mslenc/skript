package skript.ast

import skript.analysis.FunctionScope
import skript.analysis.LocalVarInfo
import skript.analysis.VarInfo
import skript.exec.ParamType
import skript.parser.Pos

sealed class Statement {
    abstract fun accept(visitor: StatementVisitor)
}

data object EmptyStatement : Statement() {
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

class WhileStatement(val condition: Expression, val body: Statement, val label: String?): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitWhile(this)
}

class DoWhileStatement(val body: Statement, val condition: Expression, val label: String?): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitDoWhile(this)
}

class ForStatement(val decls: List<VarDecl>, val container: Expression, val body: Statement, val label: String?): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitForStatement(this)
}

class VarDecl(val varName: String, val initializer: Expression?, val pos: Pos) {
    lateinit var varInfo: VarInfo

    internal fun isVarInfoDefined(): Boolean {
        return ::varInfo.isInitialized
    }
}

class LetStatement(val decls: List<VarDecl>, val export: Boolean): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitLet(this)
}

class ParamDecl(val paramName: String, val paramType: ParamType, val defaultValue: Expression?) {
    lateinit var varInfo: LocalVarInfo
}

class DeclareFunction(val funcName: String?, val params: List<ParamDecl>, val body: Statements, val pos: Pos, val export: Boolean, val implicitCtxLookup: Boolean = false) : Statement() {
    lateinit var hoistedVarInfo: VarInfo
    lateinit var innerFunScope: FunctionScope
    override fun accept(visitor: StatementVisitor) = visitor.visitDeclareFunctionStmt(this)

    fun isHoistedVarInfoDefined(): Boolean {
        return ::hoistedVarInfo.isInitialized
    }
}

class ReturnStatement(val value: Expression?) : Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitReturnStatement(this)
}

class ImportDecl(val sourceName: String?, val importedName: String, val pos: Pos) {
    lateinit var varInfo: VarInfo

    internal fun isVarInfoDefined(): Boolean {
        return ::varInfo.isInitialized
    }
}

class ImportStatement(val imports: List<ImportDecl>, val sourceName: String, val pos: Pos): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitImportStatement(this)
}

class ExportDecl(val source: Expression, val exportedName: String, val pos: Pos)

class ExportStatement(val exports: List<ExportDecl>): Statement() {
    override fun accept(visitor: StatementVisitor) = visitor.visitExportStatement(this)
}

class ExtendsStatement(val templateName: String, val pos: Pos) : Statement() {
    lateinit var rewritten: Statement
    override fun accept(visitor: StatementVisitor) = visitor.visitExtendsStatement(this)
}

/*
This is just the include "template" form, including blocks is done with BlockStatement
 */
class IncludeStatement(val templateName: String, val pos: Pos, val ctxParams: MapLiteral?): Statement() {
    lateinit var rewritten: Statement
    override fun accept(visitor: StatementVisitor) = visitor.visitIncludeStatement(this)
}

/*
block bubu => declare = true, execute = true, fromParent = false
declare block bubu => declare = true, execute = false, fromParent = false
include block bubu => declare = false, execute = true, fromParent = false
include super block => declare = false, execute = true, fromParent = true
 */
class TemplateBlockStatement(val blockName: String?, val declare: Boolean, val execute: Boolean, val fromParent: Boolean, val content: Statement?, val ctxParams: MapLiteral?, val pos: Pos): Statement() {
    lateinit var resolvedBlockName: String
    lateinit var rewritten: Statement

    override fun accept(visitor: StatementVisitor) = visitor.visitTemplateBlockStatement(this)
}