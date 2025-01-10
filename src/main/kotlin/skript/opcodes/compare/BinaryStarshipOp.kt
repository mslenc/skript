package skript.opcodes.compare

import skript.exec.Frame
import skript.opcodes.OpCodeResult
import skript.opcodes.numeric.FastBinaryOpCode
import skript.values.SkNumber
import skript.values.SkUndefined

data object BinaryStarshipOp : FastBinaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            val cmp = compare(a, b)
            val res = cmp?.let { SkNumber.valueOf(it) } ?: SkUndefined

            push(res)
        }
        return null
    }
}