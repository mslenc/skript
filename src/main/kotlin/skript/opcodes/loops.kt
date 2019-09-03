package skript.opcodes

import skript.exec.RuntimeState
import skript.values.*

interface InternalIterator {
    fun moveToNext(): Boolean
    fun getCurrentKey(): SkValue
    fun getCurrentValue(): SkValue
}

object MakeIterator: FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val container = pop()

            push(when (container) {
                is SkList -> SkListIterator(container)
                is SkMap -> SkMapIterator(container)
                is SkString -> SkStringIterator(container)
                else -> SkStringIterator(SkString.EMPTY)
            })
        }
    }
}

class IteratorNext(val pushKey: Boolean, val pushValue: Boolean, val end: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val iterator = stack.top() as InternalIterator

            if (iterator.moveToNext()) {
                if (pushKey)
                    stack.push(iterator.getCurrentKey())
                if (pushValue)
                    stack.push(iterator.getCurrentValue())
            } else {
                stack.pop()
                ip = end.value
            }
        }
    }
}