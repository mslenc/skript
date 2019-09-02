package skript.ast

import skript.analysis.VarInfo
import skript.values.SkValue

interface ExprVisitor {
    fun visitUnaryExpr(expr: UnaryExpression) { expr.inner.accept(this) }
    fun visitPrePostExpr(expr: PrePostExpr) { expr.inner.accept(this) }
    fun visitBinaryExpr(expr: BinaryExpression) { expr.left.accept(this); expr.right.accept(this) }
    fun visitAssignExpression(expr: AssignExpression) { expr.left.accept(this); expr.right.accept(this) }
    fun visitTernaryExpression(expr: TernaryExpression) { expr.cond.accept(this); expr.ifTrue.accept(this); expr.ifFalse.accept(this) }
    fun visitLiteral(expr: Literal) { }
    fun visitListLiteral(expr: ListLiteral) { expr.parts.forEach { it.value.accept(this) } }
    fun visitVarRef(expr: Variable) { }
    fun visitFieldAccess(expr: FieldAccess) { expr.obj.accept(this) }
    fun visitArrayAccess(expr: ArrayAccess) { expr.arr.accept(this); expr.index.accept(this) }

    fun visitMethodCall(expr: MethodCall) {
        expr.obj.accept(this)
        for (arg in expr.args) {
            arg.value.accept(this)
        }
    }

    fun visitFunctionCall(expr: FuncCall) {
        expr.func.accept(this)
        for (arg in expr.args) {
            arg.value.accept(this)
        }
    }

    fun visitMapLiteral(expr: MapLiteral) { expr.parts.forEach {
        if (it is MapLiteralPartExprKey)
            it.key.accept(this)

        it.value.accept(this)
    }}
}

sealed class Expression {
    abstract fun accept(visitor: ExprVisitor)
}


sealed class LValue: Expression()

enum class UnaryOp {
    MINUS,
    PLUS,
    NOT
}

class UnaryExpression(val op: UnaryOp, val inner: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitUnaryExpr(this)
    }
}

enum class PrePostOp {
    PRE_INCR,
    PRE_DECR,
    POST_INCR,
    POST_DECR
}

class PrePostExpr(val op: PrePostOp, val inner: LValue): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitPrePostExpr(this)
    }
}

enum class BinaryOp {
    MINUS,
    PLUS,
    TIMES,
    DIVIDE,
    DIVIDE_INT,
    REMAINDER,

    EQUALS,
    NOT_EQUALS,

    STRICT_EQUALS,
    NOT_STRICT_EQUALS,

    LESS_THAN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,

    OR_OR,
    AND_AND,

    ELVIS,
    STARSHIP
}

class BinaryExpression(val left: Expression, val op: BinaryOp, val right: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitBinaryExpr(this)
    }
}

class AssignExpression(val left: LValue, val op: BinaryOp?, val right: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitAssignExpression(this)
    }
}

class TernaryExpression(val cond: Expression, val ifTrue: Expression, val ifFalse: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitTernaryExpression(this)
    }
}

class Literal(val value: SkValue): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitLiteral(this)
    }
}

class ListLiteralPart(val value: Expression, val isSpread: Boolean)

class ListLiteral(val parts: List<ListLiteralPart>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitListLiteral(this)
    }
}

sealed class MapLiteralPart(val value: Expression)
class MapLiteralPartFixedKey(val key: String, value: Expression) : MapLiteralPart(value)
class MapLiteralPartExprKey(val key: Expression, value: Expression): MapLiteralPart(value)
class MapLiteralPartSpread(value: Expression): MapLiteralPart(value)

class MapLiteral(val parts: List<MapLiteralPart>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitMapLiteral(this)
    }
}

class Variable(val varName: String): LValue() {
    lateinit var varInfo: VarInfo

    override fun accept(visitor: ExprVisitor) {
        visitor.visitVarRef(this)
    }
}

class FieldAccess(val obj: Expression, val fieldName: String): LValue() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitFieldAccess(this)
    }
}

class ArrayAccess(val arr: Expression, val index: Expression): LValue() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitArrayAccess(this)
    }
}

sealed class FuncParam(val value: Expression)
class PosParam(value: Expression): FuncParam(value)
class KwParam(val name: String, value: Expression): FuncParam(value)
class SpreadPosParam(value: Expression): FuncParam(value)
class SpreadKwParam(value: Expression): FuncParam(value)

class MethodCall(val obj: Expression, val methodName: String, val args: List<FuncParam>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitMethodCall(this)
    }
}

class FuncCall(val func: Expression, val args: List<FuncParam>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitFunctionCall(this)
    }
}