package skript.opcodes.equals

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.values.*

object BinaryEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(aboutEqual(a, b)))
        }
    }
}

object BinaryNotEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!aboutEqual(a, b)))
        }
    }
}

