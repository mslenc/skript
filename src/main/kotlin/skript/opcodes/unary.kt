package skript.opcodes

import skript.exec.Frame
import skript.values.SkBoolean

abstract class FastUnaryOpCode : FastOpCode()

data object UnaryPlus : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(stack.pop().asNumber())
        }
        return null
    }
}

data object UnaryMinus : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(stack.pop().asNumber().negate())
        }
        return null
    }
}

data object UnaryNegate : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(SkBoolean.valueOf(!pop().asBoolean().value))
        }
        return null
    }
}

data object ConvertToBool : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(pop().asBoolean())
        }
        return null
    }
}

data object ConvertToString : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(pop().asString())
        }
        return null
    }
}