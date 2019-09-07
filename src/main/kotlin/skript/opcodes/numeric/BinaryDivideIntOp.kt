package skript.opcodes.numeric

import skript.values.*
import kotlin.math.floor

object BinaryDivideIntOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkScalar {
        if (right.signum() == 0)
            return SkUndefined

        return if (left is SkDecimal && right is SkDecimal) {
            SkNumber.valueOf(left.value.divideToIntegralValue(right.value))
        } else {
            SkDouble.valueOf(floor(left.toDouble() / right.toDouble()))
        }
    }
}