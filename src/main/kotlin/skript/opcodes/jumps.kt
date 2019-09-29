package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.values.SkBoolean
import skript.values.SkNull
import skript.values.SkUndefined

class Jump(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget {
        return target
    }
    override fun toString() = "Jump to ${target.value}"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.jumpCase(0, target)
    }
}

class JumpIfTruthy(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        return when {
            frame.stack.pop().asBoolean().value -> target
            else -> null
        }
    }
    override fun toString() = "JumpIfTruthy to ${target.value}"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(-1, target)
    }
}

class JumpIfFalsy(val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        return when {
            frame.stack.pop().asBoolean().value -> null
            else -> target
        }
    }
    override fun toString() = "JumpIfFalsy to ${target.value}"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(-1, target)
    }
}

class JumpIfLocalDefined(val varIndex: Int, val target: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): JumpTarget? {
        return when {
            frame.locals[varIndex] != SkUndefined -> target
            else -> null
        }
    }
    override fun toString() = "JumpIfLocalDefined to ${target.value}"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(0)
        receiver.jumpCase(0, target)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(0, target)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(0)
        receiver.jumpCase(0, target)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(0, target)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(0, target)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(0, target)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
        receiver.jumpCase(0, target)
    }
}