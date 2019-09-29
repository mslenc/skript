package skript.opcodes.compare

import skript.exec.Frame
import skript.opcodes.OpCodeResult
import skript.opcodes.numeric.FastBinaryOpCode
import skript.values.SkBoolean
import skript.values.SkUndefined

object BinaryLessThanOp : FastBinaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            val cmp = compare(a, b)
            val res = cmp?.let { SkBoolean.valueOf(cmp < 0) } ?: SkUndefined

            push(res)
        }
        return null
    }

    override fun toString() = "BinaryLessThanOp"
}