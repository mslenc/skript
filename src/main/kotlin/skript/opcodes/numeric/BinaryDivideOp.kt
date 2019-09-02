package skript.opcodes.numeric

import skript.values.SkNumber
import skript.values.SkUndefined
import skript.values.SkValue

object BinaryDivideOp : BinaryNumericOp() {
    override fun computeResult(left: SkNumber, right: SkNumber): SkValue {
        if (right.value.signum() == 0)
            return SkUndefined

        return SkNumber.valueOf(left.value / right.value)
    }
}