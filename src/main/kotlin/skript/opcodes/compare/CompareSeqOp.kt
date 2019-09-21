package skript.opcodes.compare

import skript.ast.BinaryOp
import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.opcodes.equals.aboutEqual
import skript.opcodes.equals.strictlyEqual
import skript.values.SkBoolean
import skript.values.SkUndefined
import skript.values.SkValue

class CompareSeqOp(val ops: Array<BinaryOp>): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val values = Array<SkValue>(ops.size + 1) { SkUndefined }
            for (i in ops.size downTo 0)
                values[i] = pop()

            for (i in ops.indices) {
                val left = values[i]
                val right = values[i + 1]

                val result: Boolean? = when (ops[i]) {
                    BinaryOp.LESS_THAN -> compare(left, right)?.let { it < 0 }
                    BinaryOp.LESS_OR_EQUAL -> compare(left, right)?.let { it <= 0 }
                    BinaryOp.GREATER_THAN -> compare(left, right)?.let { it > 0 }
                    BinaryOp.GREATER_OR_EQUAL -> compare(left, right)?.let { it >= 0 }
                    BinaryOp.EQUALS -> aboutEqual(left, right)
                    BinaryOp.STRICT_EQUALS -> strictlyEqual(left, right)
                    else -> throw IllegalStateException()
                }

                when (result) {
                    null -> {
                        push(SkUndefined)
                        return
                    }
                    false -> {
                        push(SkBoolean.FALSE)
                        return
                    }
                    else -> {
                        // continue..
                    }
                }
            }

            push(SkBoolean.TRUE)
            return
        }
    }

    override fun toString() = "CompareSeqOp ops=$ops"
}