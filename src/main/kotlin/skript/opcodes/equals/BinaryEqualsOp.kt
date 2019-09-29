package skript.opcodes.equals

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.opcodes.OpCodeResult
import skript.values.*

object BinaryEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(aboutEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryEqualsOp"
}

object BinaryNotEqualsOp : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!aboutEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryNotEqualsOp"
}

