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
        state.topFrame.apply {
            stack.push(SkBoolean.valueOf(!stack.pop().asBoolean().value))
        }
    }
}