package skript.opcodes.compare

import skript.exec.Frame
import skript.opcodes.FastOpCode
import skript.opcodes.OpCodeResult
import skript.values.SkNumber
import skript.values.SkUndefined

object BinaryStarshipOp : FastOpCode() {
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

    override fun toString() = "BinaryStarshipOp"
}