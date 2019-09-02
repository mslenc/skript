package skript.opcodes.numeric

import skript.values.SkNumber
import skript.values.SkValue

object BinaryMultiplyOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkValue {
        return SkNumber.valueOf(left.value * right.value)
    }
}