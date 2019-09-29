package skript.opcodes.equals

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.opcodes.OpCodeResult
import skript.values.*

object BinaryStrictEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(strictlyEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryStrictEqualsOp"
}

object BinaryStrictNotEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!strictlyEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryStrictNotEqualsOp"
}

