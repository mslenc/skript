package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkBoolean
import skript.values.SkNull
import skript.values.SkUndefined

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

class JumpForSafeMethodCall(val target: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val obj = stack.top()

            when (obj) {
                SkUndefined -> ip = target.value
                SkNull -> { stack.pop(); stack.push(SkUndefined); ip = target.value }
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

class JumpIfTopTruthyElseDropAlsoMakeBool(val target: JumpTarget): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            if (value.asBoolean().value) {
                stack.push(SkBoolean.TRUE)
                ip = target.value
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

class JumpIfTopFalsyElseDropAlsoMakeBool(val target: JumpTarget): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            if (!value.asBoolean().value) {
                stack.push(SkBoolean.FALSE)
                ip = target.value
            }
        }
    }
}