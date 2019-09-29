package skript.opcodes

import skript.exec.Frame
import skript.values.SkBoolean

object UnaryPlus : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(stack.pop().asNumber())
        }
        return null
    }
    override fun toString() = "UnaryPlus"
}

object UnaryMinus : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(stack.pop().asNumber().negate())
        }
        return null
    }
    override fun toString() = "UnaryMinus"
}

object UnaryNegate : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(SkBoolean.valueOf(!pop().asBoolean().value))
        }
        return null
    }
    override fun toString() = "UnaryNegate"
}

object ConvertToBool : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(pop().asBoolean())
        }
        return null
    }
    override fun toString() = "ConvertToBool"
}

object ConvertToString : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(pop().asString())
        }
        return null
    }
    override fun toString() = "ConvertToString"
}