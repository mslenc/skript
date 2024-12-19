package skript.ast

interface StatementVisitor {
    fun visitBlock(stmts: Statements)
    fun visitIf(stmt: IfStatement)
    fun visitExprStmt(stmt: ExpressionStatement)
    fun visitWhile(stmt: WhileStatement)
    fun visitDoWhile(stmt: DoWhileStatement)
    fun visitForStatement(stmt: ForStatement)
    fun visitLet(stmt: LetStatement)
    fun visitDeclareFunctionStmt(stmt: DeclareFunction)
    fun visitImportStatement(stmt: ImportStatement)
    fun visitExportStatement(stmt: ExportStatement)
    fun visitReturnStatement(stmt: ReturnStatement)
    fun visitBreakStatement(stmt: BreakStatement)
    fun visitContinueStatement(stmt: ContinueStatement)
    fun visitExtendsStatement(stmt: ExtendsStatement)
    fun visitIncludeStatement(stmt: IncludeStatement)
    fun visitTemplateBlockStatement(stmt: TemplateBlockStatement)
}

abstract class AbstractStatementVisitor : StatementVisitor {
    override fun visitBlock(stmts: Statements) {
        for (stmt in stmts.parts) {
            stmt.accept(this)
        }
    }

    override fun visitIf(stmt: IfStatement) {
        stmt.ifTrue.accept(this)
        stmt.ifFalse?.accept(this)
    }

    override fun visitExprStmt(stmt: ExpressionStatement) {
        // nothing
    }

    override fun visitWhile(stmt: WhileStatement) {
        stmt.body.accept(this)
    }

    override fun visitDoWhile(stmt: DoWhileStatement) {
        stmt.body.accept(this)
    }

    override fun visitForStatement(stmt: ForStatement) {
        stmt.body.accept(this)
    }

    override fun visitLet(stmt: LetStatement) {
        // nothing
    }

    override fun visitDeclareFunctionStmt(stmt: DeclareFunction) {
        stmt.body.accept(this)
    }

    override fun visitImportStatement(stmt: ImportStatement) {
        // nothing
    }

    override fun visitExportStatement(stmt: ExportStatement) {
        // nothing
    }

    override fun visitReturnStatement(stmt: ReturnStatement) {
        // nothing
    }

    override fun visitBreakStatement(stmt: BreakStatement) {
        // nothing
    }

    override fun visitContinueStatement(stmt: ContinueStatement) {
        // nothing
    }

    override fun visitExtendsStatement(stmt: ExtendsStatement) {
        // nothing
    }

    override fun visitIncludeStatement(stmt: IncludeStatement) {
        // nothing
    }

    override fun visitTemplateBlockStatement(stmt: TemplateBlockStatement) {
        stmt.content?.accept(this)
    }
}