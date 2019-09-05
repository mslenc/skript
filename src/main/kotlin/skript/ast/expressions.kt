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

    fun visitCompareAllPairs(expr: CompareAllPairs) {
        for (operand in expr.operands) {
            operand.accept(this)
        }
    }

    fun visitCompareSequence(expr: CompareSequence) {
        for (operand in expr.operands) {
            operand.accept(this)
        }
    }

    fun visitObjectIs(expr: ObjectIs) {
        expr.obj.accept(this)
        expr.klass.accept(this)
    }

    fun visitValueIn(expr: ValueIn) {
        expr.value.accept(this)
        expr.container.accept(this)
    }

    fun visitRange(expr: Range) {
        expr.start.accept(this)
        expr.end.accept(this)
    }

    fun visitFunctionLiteral(expr: FunctionLiteral) {
    }
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
    SUBTRACT,
    ADD,
    MULTIPLY,
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

    OR,
    AND,
    OR_OR,
    AND_AND,

    ELVIS,
    STARSHIP,

    RANGE_TO,
    RANGE_TO_EXCL
}

class BinaryExpression(val left: Expression, val op: BinaryOp, val right: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitBinaryExpr(this)
    }
}

class CompareSequence(val operands: List<Expression>, val ops: List<BinaryOp>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitCompareSequence(this)
    }
}

class CompareAllPairs(val operands: List<Expression>, val ops: List<BinaryOp>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitCompareAllPairs(this)
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

sealed class CallArg(val value: Expression)
class PosArg(value: Expression): CallArg(value)
class KwArg(val name: String, value: Expression): CallArg(value)
class SpreadPosArg(value: Expression): CallArg(value)
class SpreadKwArg(value: Expression): CallArg(value)

enum class MethodCallType {
    REGULAR, // obj.ident(...)
    SAFE,    // obj?.ident(...)
    INFIX,   // obj ident arg
    OPERATOR // obj += arg
}

class MethodCall(val obj: Expression, val methodName: String, val args: List<CallArg>, val type: MethodCallType): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitMethodCall(this)
    }
}

class FuncCall(val func: Expression, val args: List<CallArg>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitFunctionCall(this)
    }
}

class ObjectIs(val obj: Expression, val klass: Expression, val positive: Boolean): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitObjectIs(this)
    }
}

class ValueIn(val value: Expression, val container: Expression, val positive: Boolean): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitValueIn(this)
    }
}

class Range(val start: Expression, val end: Expression, val endInclusive: Boolean): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitRange(this);
    }
}

class FunctionLiteral(val funDecl: DeclareFunction): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitFunctionLiteral(this)
    }
}