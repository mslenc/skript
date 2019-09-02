package skript.opcodes.compare

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.values.SkNumber
import skript.values.SkUndefined

object BinaryStarshipOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            val cmp = compare(a, b)
            val res = cmp?.let { SkNumber.valueOf(it) } ?: SkUndefined

            push(res)
        }
    }
}