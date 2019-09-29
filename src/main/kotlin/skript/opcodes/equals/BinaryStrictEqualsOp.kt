package skript.opcodes.equals

import skript.exec.Frame
import skript.opcodes.FastOpCode
import skript.opcodes.OpCodeResult
import skript.values.*

object BinaryStrictEqualsOp : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(strictlyEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryStrictEqualsOp"
}

object BinaryStrictNotEqualsOp : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!strictlyEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryStrictNotEqualsOp"
}

