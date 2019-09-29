package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.values.SkBoolean

abstract class FastUnaryOpCode : FastOpCode() {
    final override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(0)
    }
}

object UnaryPlus : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(stack.pop().asNumber())
        }
        return null
    }
    override fun toString() = "UnaryPlus"
}

object UnaryMinus : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(stack.pop().asNumber().negate())
        }
        return null
    }
    override fun toString() = "UnaryMinus"
}

object UnaryNegate : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(SkBoolean.valueOf(!pop().asBoolean().value))
        }
        return null
    }
    override fun toString() = "UnaryNegate"
}

object ConvertToBool : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(pop().asBoolean())
        }
        return null
    }
    override fun toString() = "ConvertToBool"
}

object ConvertToString : FastUnaryOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(pop().asString())
        }
        return null
    }
    override fun toString() = "ConvertToString"
}