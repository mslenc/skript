package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkBoolean

object UnaryPlus : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(stack.pop().asNumber())
        }
    }
    override fun toString() = "UnaryPlus"
}

object UnaryMinus : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(stack.pop().asNumber().negate())
        }
    }
    override fun toString() = "UnaryMinus"
}

object UnaryNegate : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(SkBoolean.valueOf(!pop().asBoolean().value))
        }
    }
    override fun toString() = "UnaryNegate"
}

object ConvertToBool : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(pop().asBoolean())
        }
    }
    override fun toString() = "ConvertToBool"
}

object ConvertToString : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(pop().asString())
        }
    }
    override fun toString() = "ConvertToString"
}