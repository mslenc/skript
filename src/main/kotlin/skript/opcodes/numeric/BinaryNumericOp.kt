package skript.opcodes.numeric

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.opcodes.OpCodeResult
import skript.values.*
import java.math.BigDecimal

abstract class BinaryNumericOp : FastOpCode() {
    abstract fun computeResult(left: Double, right: Double): SkDouble?
    abstract fun computeResult(left: BigDecimal, right: BigDecimal): SkDecimal?

    private fun compute(left: SkValue, right: SkValue): SkScalar {
        if (left == SkUndefined || right == SkUndefined)
            return SkUndefined

        val leftNum = left.asNumber()
        val rightNum = right.asNumber()

        return if (leftNum is SkDecimal && rightNum is SkDecimal) {
            computeResult(
                leftNum.toBigDecimal(),
                rightNum.toBigDecimal()
            ) ?: return SkUndefined
        } else {
            val result = computeResult(
                leftNum.toDouble().also { if (it.isNaN()) return SkUndefined },
                rightNum.toDouble().also { if (it.isNaN()) return SkUndefined }
            ) ?: return SkUndefined

            result.dvalue.let {
                if (it.isNaN() || it.isInfinite()) {
                    return SkUndefined
                }
            }

            result
        }
    }

    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            val right = stack.pop()
            val left = stack.pop()
            stack.push(compute(left, right))
        }
        return null
    }

    override fun toString() = this::class.simpleName ?: "???"
}