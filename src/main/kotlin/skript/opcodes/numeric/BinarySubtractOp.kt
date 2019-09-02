package skript.opcodes.numeric

import skript.values.SkNumber

object BinarySubtractOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkNumber {
        return SkNumber.valueOf(left.value - right.value)
    }
}