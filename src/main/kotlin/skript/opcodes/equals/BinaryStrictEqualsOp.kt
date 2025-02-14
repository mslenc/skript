package skript.opcodes.equals

import skript.exec.Frame
import skript.opcodes.OpCodeResult
import skript.opcodes.numeric.FastBinaryOpCode
import skript.values.*

data object BinaryStrictEqualsOp : FastBinaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(strictlyEqual(a, b)))
        }
        return null
    }
}

data object BinaryStrictNotEqualsOp : FastBinaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!strictlyEqual(a, b)))
        }
        return null
    }
}

