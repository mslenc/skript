package skript.opcodes.equals

import skript.exec.Frame
import skript.opcodes.FastOpCode
import skript.opcodes.OpCodeResult
import skript.values.*

object BinaryEqualsOp : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(aboutEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryEqualsOp"
}

object BinaryNotEqualsOp : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val b = pop()
            val a = pop()

            push(SkBoolean.valueOf(!aboutEqual(a, b)))
        }
        return null
    }

    override fun toString() = "BinaryNotEqualsOp"
}

