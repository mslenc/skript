package skript.opcodes.numeric

import skript.values.*
import java.math.BigDecimal

object BinaryMultiplyOp : BinaryNumericOp() {
    override fun computeResult(left: Double, right: Double): SkDouble {
        return SkDouble.valueOf(left * right)
    }

    override fun computeResult(left: BigDecimal, right: BigDecimal): SkDecimal {
        return SkDecimal.valueOf(left * right)
    }
}