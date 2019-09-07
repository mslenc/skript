package skript.opcodes.numeric

import skript.values.*
import java.math.BigDecimal

object BinaryRemainderOp : BinaryNumericOp() {
    override fun computeResult(left: Double, right: Double): SkDouble? {
        if (right == 0.0)
            return null

        return SkDouble.valueOf(left % right)
    }

    override fun computeResult(left: BigDecimal, right: BigDecimal): SkDecimal? {
        if (right.signum() == 0)
            return null

        return SkDecimal.valueOf(left % right)
    }
}