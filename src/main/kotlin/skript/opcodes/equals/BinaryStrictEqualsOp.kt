package skript.opcodes.equals

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.values.*

object BinaryStrictEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(strictlyEqual(a, b)))
        }
    }
}

object BinaryStrictNotEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!strictlyEqual(a, b)))
        }
    }
}

