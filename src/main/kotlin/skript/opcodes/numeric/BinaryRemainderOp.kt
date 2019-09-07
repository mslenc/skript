package skript.opcodes.numeric

import skript.values.*

object BinaryRemainderOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkScalar {
        if (right.signum() == 0)
            return SkUndefined

        return if (left is SkDecimal && right is SkDecimal) {
            SkNumber.valueOf(left.value % right.value)
        } else {
            SkDouble.valueOf(left.toDouble() % right.toDouble())
        }
    }
}