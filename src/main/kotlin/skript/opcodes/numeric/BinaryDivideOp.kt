package skript.opcodes.numeric

import skript.values.*

object BinaryDivideOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkScalar {
        if (right.signum() == 0)
            return SkUndefined

        return if (left is SkDecimal && right is SkDecimal) {
            SkDecimal.valueOf(left.value / right.value)
        } else {
            SkDouble.valueOf(left.toDouble() / right.toDouble())
        }
    }
}