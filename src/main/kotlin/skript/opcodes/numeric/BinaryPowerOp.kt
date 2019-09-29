package skript.opcodes.numeric

import skript.isInteger
import skript.values.SkDecimal
import skript.values.SkDouble
import java.math.BigDecimal
import kotlin.math.pow

private val MAX_EXP = 999999999.toBigDecimal()

object BinaryPowerOp : BinaryNumericOp() {
    override fun computeResult(left: Double, right: Double): SkDouble {
        return SkDouble.valueOf(left.pow(right))
    }

    override fun computeResult(left: BigDecimal, right: BigDecimal): SkDecimal {
        return if (right.signum() >= 0 && right.isInteger() && right <= MAX_EXP) {
            SkDecimal.valueOf(left.pow(right.toInt()))
        } else {
            SkDecimal.valueOf(left.toDouble().pow(right.toDouble()))
        }
    }
}