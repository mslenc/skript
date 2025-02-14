package skript.opcodes.equals

import skript.exec.Frame
import skript.opcodes.OpCodeResult
import skript.opcodes.numeric.FastBinaryOpCode
import skript.values.*

data object BinaryEqualsOp : FastBinaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(aboutEqual(a, b)))
        }
        return null
    }
}

data object BinaryNotEqualsOp : FastBinaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!aboutEqual(a, b)))
        }
        return null
    }
}

