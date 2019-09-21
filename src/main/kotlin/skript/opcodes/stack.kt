package skript.opcodes

import skript.exec.RuntimeState

object Dup : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(top())
        }
    }
    override fun toString() = "Dup"
}

object Dup2 : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            push(top(1))
            push(top(1))
        }
    }
    override fun toString() = "Dup2"
}

object Pop : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.pop()
    }
    override fun toString() = "Pop"
}

object CopyTopTwoDown : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val value = pop()
            val obj = pop()

            push(value)
            push(obj)
            push(value)
        }
    }
    override fun toString() = "CopyTopTwoDown"
}

object CopyTopThreeDown : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val value = pop()
            val index = pop()
            val arr = pop()

            push(value)
            push(arr)
            push(index)
            push(value)
        }
    }
    override fun toString() = "CopyTopThreeDown"
}