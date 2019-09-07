package skript.opcodes.numeric

import skript.values.SkDecimal
import skript.values.SkDouble
import skript.values.SkNumber

object BinarySubtractOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkNumber {
        return if (left is SkDecimal && right is SkDecimal) {
            SkDecimal.valueOf(left.value - right.value)
        } else {
            SkDouble.valueOf(left.toDouble() - right.toDouble())
        }
    }
}