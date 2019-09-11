package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkBoolean

object UnaryPlus : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(stack.pop().asNumber())
        }
    }
}

object UnaryMinus : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(stack.pop().asNumber().negate())
        }
    }
}

object UnaryNegate : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(SkBoolean.valueOf(!pop().asBoolean().value))
        }
    }
}

object ConvertToBool : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(pop().asBoolean())
        }
    }
}

object ConvertToString : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(pop().asString())
        }
    }
}