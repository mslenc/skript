package skript.opcodes

import skript.exec.RuntimeState
import skript.lexer.Pos
import skript.typeError
import skript.values.*

interface SkIterator {
    fun moveToNext(): Boolean
    fun getCurrentKey(): SkValue
    fun getCurrentValue(): SkValue
}

object MakeIterator: SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.stack.apply {
            val container = pop()
            val iterator = container.makeIterator()

            if (iterator is SkIterator) {
                push(iterator)
            } else {
                typeError("Can't iterate over " + container.asString().value, Pos(0, 0, "TODO"))
            }
        }
    }
}

class IteratorNext(val pushKey: Boolean, val pushValue: Boolean, val end: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val iterator = stack.top() as SkIterator

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

class MakeRangeEndInclusive(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.stack.apply {
            val to = pop()
            val from = pop()
            push(from.makeRange(to, true, state, exprDebug))
        }
    }
}

class MakeRangeEndExclusive(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.stack.apply {
            val to = pop()
            val from = pop()
            push(from.makeRange(to, false, state, exprDebug))
        }
    }
}