package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkNull
import skript.values.SkUndefined

class JumpTarget {
    var value: Int = -1
}

class Jump(val target: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.ip = target.value
    }
}

class JumpIfTruthy(val target: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            if (value.asBoolean().value) {
                ip = target.value
            }
        }
    }
}

class JumpIfFalsy(val target: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            if (!value.asBoolean().value) {
                ip = target.value
            }
        }
    }
}

class JumpIfLocalDefined(val varIndex: Int, val target: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            if (locals[varIndex] != SkUndefined) {
                ip = target.value
            }
        }
    }
}

class JumpIfTopDefinedElseDrop(val target: JumpTarget): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.top()

            if (value == SkUndefined || value == SkNull) {
                stack.pop()
            } else {
                ip = target.value
            }
        }
    }
}

class JumpIfTopTruthyElseDrop(val target: JumpTarget): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {

            if (stack.top().asBoolean().value) {
                ip = target.value
            } else {
                stack.pop()
            }
        }
    }
}

class JumpIfTopFalsyElseDrop(val target: JumpTarget): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {

            if (stack.top().asBoolean().value) {
                stack.pop()
            } else {
                ip = target.value
            }
        }
    }
}