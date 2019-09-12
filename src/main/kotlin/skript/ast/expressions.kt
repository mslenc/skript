package skript.ast

import skript.analysis.VarInfo
import skript.parser.Pos
import skript.values.SkScalar

interface ExprVisitor {
    fun visitUnaryExpr(expr: UnaryExpression) { expr.inner.accept(this) }
    fun visitPrePostExpr(expr: PrePostExpr) { expr.inner.accept(this) }
    fun visitBinaryExpr(expr: BinaryExpression) { expr.left.accept(this); expr.right.accept(this) }
    fun visitAssignExpression(expr: AssignExpression) { expr.left.accept(this); expr.right.accept(this) }
    fun visitTernaryExpression(expr: TernaryExpression) { expr.cond.accept(this); expr.ifTrue.accept(this); expr.ifFalse.accept(this) }
    fun visitLiteral(expr: Literal) { }
    fun visitListLiteral(expr: ListLiteral) { expr.parts.forEach { it.value.accept(this) } }
    fun visitVarRef(expr: Variable) { }
    fun visitPropertyAccess(expr: PropertyAccess) { expr.obj.accept(this) }
    fun visitElementAccess(expr: ElementAccess) { expr.arr.accept(this); expr.index.accept(this) }
    fun visitParentheses(expr: Parentheses) { expr.inner.accept(this) }

    fun visitStringTemplate(expr: StringTemplateExpr) {
        for (part in expr.parts) {
            if (part is StrTemplateExpr) {
                part.expr.accept(this)
            }
        }
    }

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

    fun visitFunctionLiteral(expr: FunctionLiteral)
}

sealed class Expression {
    abstract fun accept(visitor: ExprVisitor)

    abstract fun toString(sb: StringBuilder)

    override fun toString(): String {
        return StringBuilder().apply { toString(this) }.toString()
    }
}


sealed class LValue: Expression()

enum class UnaryOp(val strRep: String) {
    MINUS("-"),
    PLUS("+"),
    NOT("!");
}

class Parentheses(val inner: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitParentheses(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append('(')
        inner.toString(sb)
        sb.append(')')
    }
}

class UnaryExpression(val op: UnaryOp, val inner: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitUnaryExpr(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append(op.strRep)
        inner.toString(sb)
    }
}

enum class PrePostOp(val strRep: String) {
    PRE_INCR("++"),
    PRE_DECR("--"),
    POST_INCR("++"),
    POST_DECR("--")
}

class PrePostExpr(val op: PrePostOp, val inner: LValue): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitPrePostExpr(this)
    }

    override fun toString(sb: StringBuilder) {
        if (op == PrePostOp.PRE_INCR || op == PrePostOp.PRE_DECR)
            sb.append(op.strRep)

        inner.toString(sb)

        if (op == PrePostOp.POST_INCR || op == PrePostOp.POST_DECR)
            sb.append(op.strRep)
    }
}

enum class BinaryOp(val strRep: String) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    DIVIDE_INT("//"),
    REMAINDER("%"),

    EQUALS("=="),
    NOT_EQUALS("!="),

    STRICT_EQUALS("==="),
    NOT_STRICT_EQUALS("!=="),

    LESS_THAN("<"),
    LESS_OR_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_OR_EQUAL(">="),

    OR("|"),
    AND("&"),
    OR_OR("||"),
    AND_AND("&&"),

    ELVIS("?:"),
    STARSHIP("<=>"),

    RANGE_TO(".."),
    RANGE_TO_EXCL("..<")
}

class BinaryExpression(val pos: Pos, val left: Expression, val op: BinaryOp, val right: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitBinaryExpr(this)
    }

    override fun toString(sb: StringBuilder) {
        left.toString(sb)
        sb.append(op.strRep)
        right.toString(sb)
    }
}

class CompareSequence(val operands: List<Expression>, val ops: List<BinaryOp>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitCompareSequence(this)
    }

    override fun toString(sb: StringBuilder) {
        operands[0].toString(sb)

        for (i in ops.indices) {
            sb.append(ops[i].strRep)
            operands[i + 1].toString(sb)
        }
    }
}

class CompareAllPairs(val operands: List<Expression>, val ops: List<BinaryOp>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitCompareAllPairs(this)
    }

    override fun toString(sb: StringBuilder) {
        operands[0].toString(sb)

        for (i in ops.indices) {
            sb.append(ops[i].strRep)
            operands[i + 1].toString(sb)
        }
    }
}

class AssignExpression(val left: LValue, val op: BinaryOp?, val right: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitAssignExpression(this)
    }

    override fun toString(sb: StringBuilder) {
        left.toString(sb)
        op?.let { sb.append(it.strRep) }
        sb.append("=")
        right.toString(sb)
    }
}

class TernaryExpression(val cond: Expression, val ifTrue: Expression, val ifFalse: Expression): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitTernaryExpression(this)
    }

    override fun toString(sb: StringBuilder) {
        cond.toString(sb)
        sb.append("?")
        ifTrue.toString(sb)
        sb.append(":")
        ifFalse.toString(sb)
    }
}

class Literal(val value: SkScalar): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitLiteral(this)
    }

    override fun toString(sb: StringBuilder) {
        value.toString(sb)
    }
}

class ListLiteralPart(val value: Expression, val isSpread: Boolean) {
    fun toString(sb: StringBuilder) {
        if (isSpread)
            sb.append('*')
        value.toString(sb)
    }
}

class ListLiteral(val parts: List<ListLiteralPart>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitListLiteral(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append('[')
        for (i in parts.indices) {
            if (i > 0) sb.append(",")
            parts[i].toString(sb)
        }
        sb.append(']')
    }
}

sealed class MapLiteralPart(val value: Expression) {
    abstract fun toString(sb: StringBuilder)
}

class MapLiteralPartFixedKey(val key: String, value: Expression) : MapLiteralPart(value) {
    override fun toString(sb: StringBuilder) {
        sb.append(key).append(':')
        value.toString(sb)
    }
}

class MapLiteralPartExprKey(val key: Expression, value: Expression): MapLiteralPart(value) {
    override fun toString(sb: StringBuilder) {
        sb.append("(")
        key.toString(sb)
        sb.append("):")
        value.toString(sb)
    }
}

class MapLiteralPartSpread(value: Expression): MapLiteralPart(value) {
    override fun toString(sb: StringBuilder) {
        sb.append("**")
        value.toString(sb)
    }
}

class MapLiteral(val parts: List<MapLiteralPart>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitMapLiteral(this)
    }

    override fun toString(sb: StringBuilder) {
        if (parts.isEmpty()) {
            sb.append("[:]")
            return
        }

        sb.append('[')
        for (i in parts.indices) {
            if (i > 0)
                sb.append(",")
            parts[i].toString(sb)
        }
        sb.append(']')
    }
}

class Variable(val varName: String, val pos: Pos): LValue() {
    lateinit var varInfo: VarInfo

    override fun accept(visitor: ExprVisitor) {
        visitor.visitVarRef(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append(varName)
    }
}

class PropertyAccess(val obj: Expression, val propName: String): LValue() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitPropertyAccess(this)
    }

    override fun toString(sb: StringBuilder) {
        obj.toString(sb)
        sb.append(".").append(propName)
    }
}

class ElementAccess(val arr: Expression, val index: Expression): LValue() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitElementAccess(this)
    }

    override fun toString(sb: StringBuilder) {
        arr.toString(sb)
        sb.append('[')
        index.toString(sb)
        sb.append(']')
    }
}

sealed class CallArg(val value: Expression) {
    abstract fun toString(sb: StringBuilder)
}

class PosArg(value: Expression): CallArg(value) {
    override fun toString(sb: StringBuilder) {
        value.toString(sb)
    }
}

class KwArg(val name: String, value: Expression): CallArg(value) {
    override fun toString(sb: StringBuilder) {
        sb.append(name).append("=")
        value.toString()
    }
}

class SpreadPosArg(value: Expression): CallArg(value) {
    override fun toString(sb: StringBuilder) {
        sb.append("*")
        value.toString(sb)
    }
}

class SpreadKwArg(value: Expression): CallArg(value) {
    override fun toString(sb: StringBuilder) {
        sb.append("**")
        value.toString(sb)
    }
}

enum class MethodCallType {
    REGULAR, // obj.ident(...)
    SAFE,    // obj?.ident(...)
    INFIX   // obj ident arg
}

fun List<CallArg>.toString(sb: StringBuilder) {
    sb.append('(')
    for (i in indices) {
        if (i > 0)
            sb.append(',')
        this[i].toString(sb)
    }
    sb.append(')')
}

class MethodCall(val obj: Expression, val methodName: String, val args: List<CallArg>, val type: MethodCallType): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitMethodCall(this)
    }

    override fun toString(sb: StringBuilder) {
        obj.toString(sb)

        when (type) {
            MethodCallType.REGULAR -> {
                sb.append(".").append(methodName)
                args.toString(sb)
            }

            MethodCallType.SAFE -> {
                sb.append("?.").append(methodName)
                args.toString(sb)
            }

            MethodCallType.INFIX -> {
                sb.append(" ").append(methodName).append(" ")
                args.single().toString(sb)
            }
        }
    }
}

class FuncCall(val func: Expression, val args: List<CallArg>): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitFunctionCall(this)
    }

    override fun toString(sb: StringBuilder) {
        func.toString(sb)
        args.toString(sb)
    }
}

class ObjectIs(val obj: Expression, val klass: Expression, val positive: Boolean): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitObjectIs(this)
    }

    override fun toString(sb: StringBuilder) {
        obj.toString(sb)
        sb.append(if (positive) " is " else " !is ")
        klass.toString(sb)
    }
}

class ValueIn(val value: Expression, val container: Expression, val positive: Boolean): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitValueIn(this)
    }

    override fun toString(sb: StringBuilder) {
        value.toString(sb)
        sb.append(if (positive) " !in " else " !in ")
        container.toString(sb)
    }
}

class Range(val start: Expression, val end: Expression, val endInclusive: Boolean): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitRange(this)
    }

    override fun toString(sb: StringBuilder) {
        start.toString(sb)
        sb.append(if (endInclusive) ".." else "..<")
        end.toString(sb)
    }
}

class FunctionLiteral(val funDecl: DeclareFunction): Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitFunctionLiteral(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append("fun(...){...}") // TODO
    }
}

sealed class StrTemplatePart
class StrTemplateText(val text: String) : StrTemplatePart()
class StrTemplateExpr(val expr: Expression) : StrTemplatePart()

class StringTemplateExpr(val parts: List<StrTemplatePart>) : Expression() {
    override fun accept(visitor: ExprVisitor) {
        visitor.visitStringTemplate(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append('`')
        for (part in parts) {
            when (part) {
                is StrTemplateText -> {
                    sb.append(part.text)
                }
                is StrTemplateExpr -> {
                    sb.append("\${")
                    part.expr.toString(sb)
                    sb.append("}")
                }
            }
        }
    }
}