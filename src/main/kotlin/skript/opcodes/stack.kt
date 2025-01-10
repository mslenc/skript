package skript.opcodes

import skript.exec.Frame

data object Dup : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(top())
        }
        return null
    }
}

data object Dup2 : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            push(top(1))
            push(top(1))
        }
        return null
    }
}

data object Pop : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.pop()
        return null
    }
}

data object CopyTopTwoDown : FastOpCode() {
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
}

data object CopyTopThreeDown : FastOpCode() {
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
}