package skript.opcodes

import skript.exec.Frame
import skript.values.SkBoolean
import skript.values.SkNull
import skript.values.SkUndefined

class Jump(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget {
        return target
    }
    override fun toString() = "Jump to ${target.value}"
}

class JumpIfTruthy(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        return when {
            frame.stack.pop().asBoolean().value -> target
            else -> null
        }
    }
    override fun toString() = "JumpIfTruthy to ${target.value}"
}

class JumpIfFalsy(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        return when {
            frame.stack.pop().asBoolean().value -> null
            else -> target
        }
    }
    override fun toString() = "JumpIfFalsy to ${target.value}"
}

class JumpIfLocalDefined(val varIndex: Int, val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        return when {
            frame.locals[varIndex] != SkUndefined -> target
            else -> null
        }
    }
    override fun toString() = "JumpIfLocalDefined to ${target.value}"
}

class JumpIfTopDefinedElseDrop(val target: JumpTarget): FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        frame.apply {
            val value = stack.top()

            if (value == SkUndefined || value == SkNull) {
                stack.pop()
            } else {
                return target
            }
        }
        return null
    }
    override fun toString() = "JumpIfTopDefinedElseDrop to ${target.value}"
}

class JumpForSafeMethodCall(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        frame.apply {
            return when (stack.top()) {
                SkUndefined -> target
                SkNull -> { stack.pop(); stack.push(SkUndefined); target }
                else -> null
            }
        }
    }
    override fun toString() = "JumpForSafeMethodCall to ${target.value}"
}

class JumpIfTopTruthyElseDrop(val target: JumpTarget): FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        frame.apply {
            return when {
                stack.top().asBoolean().value -> target
                else -> { stack.pop(); null }
            }
        }
    }
    override fun toString() = "JumpIfTopTruthyElseDrop to ${target.value}"
}

class JumpIfTopTruthyElseDropAlsoMakeBool(val target: JumpTarget): FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        frame.apply {
            val value = stack.pop()
            if (value.asBoolean().value) {
                stack.push(SkBoolean.TRUE)
                return target
            }
        }
        return null
    }
    override fun toString() = "JumpIfTopTruthyElseDropAlsoMakeBool to ${target.value}"
}

class JumpIfTopFalsyElseDrop(val target: JumpTarget): FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        frame.apply {
            if (stack.top().asBoolean().value) {
                stack.pop()
            } else {
                return target
            }
        }
        return null
    }
    override fun toString() = "JumpIfTopFalsyElseDrop to ${target.value}"
}

class JumpIfTopFalsyElseDropAlsoMakeBool(val target: JumpTarget): FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        frame.apply {
            val value = stack.pop()
            if (!value.asBoolean().value) {
                stack.push(SkBoolean.FALSE)
                return target
            }
        }
        return null
    }
    override fun toString() = "JumpIfTopFalsyElseDropAlsoMakeBool to ${target.value}"
}