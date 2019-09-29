package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame

object Dup : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(top())
        }
        return null
    }
    override fun toString() = "Dup"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

object Dup2 : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(top(1))
            push(top(1))
        }
        return null
    }
    override fun toString() = "Dup2"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(2)
    }
}

object Pop : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.pop()
        return null
    }
    override fun toString() = "Pop"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}

object CopyTopTwoDown : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val value = pop()
            val obj = pop()

            push(value)
            push(obj)
            push(value)
        }
        return null
    }
    override fun toString() = "CopyTopTwoDown"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

object CopyTopThreeDown : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val value = pop()
            val index = pop()
            val arr = pop()

            push(value)
            push(arr)
            push(index)
            push(value)
        }
        return null
    }
    override fun toString() = "CopyTopThreeDown"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}