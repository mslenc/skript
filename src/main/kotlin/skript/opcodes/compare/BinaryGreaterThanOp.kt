package skript.opcodes.compare

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.values.SkBoolean
import skript.values.SkUndefined

object BinaryGreaterThanOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            val cmp = compare(a, b)
            val res = cmp?.let { SkBoolean.valueOf(cmp > 0) } ?: SkUndefined

            push(res)
        }
    }
}