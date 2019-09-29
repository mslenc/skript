package skript.opcodes

import skript.exec.RuntimeState
import skript.io.toSkript
import skript.parser.Pos
import skript.typeError
import skript.values.*

abstract class SkIterator : SkObject() {
    abstract fun moveToNext(): Boolean
    abstract fun getCurrentKey(): SkValue
    abstract fun getCurrentValue(): SkValue
}

object SkIteratorClassDef : SkCustomClass<SkIterator>("Iterator") {
    init {
        defineMethod("moveToNext").withImpl {
            it.moveToNext().toSkript()
        }

        defineReadOnlyProperty("currentKey") { it.getCurrentKey() }
        defineReadOnlyProperty("currentValue") { it.getCurrentValue() }
    }
}

object MakeIterator: SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val container = pop()
            val iterator = container.makeIterator() ?: typeError("Can't iterate over " + container.asString().value, Pos(0, 0, "TODO"))
            push(iterator)
        }
        return null
    }
    override fun toString() = "MakeIterator"
}

class IteratorNext(val pushKey: Boolean, val pushValue: Boolean, val end: JumpTarget) : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
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
        return null
    }
    override fun toString() = "IteratorNext end=${end.value} pushKey=${pushKey} pushValue=${pushValue}"
}

class MakeRangeEndInclusive(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val to = pop()
            val from = pop()
            push(from.makeRange(to, true, state, exprDebug))
        }
        return null
    }
    override fun toString() = "MakeRangeEndInclusive expr=$exprDebug"
}

class MakeRangeEndExclusive(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val to = pop()
            val from = pop()
            push(from.makeRange(to, false, state, exprDebug))
        }
        return null
    }
    override fun toString() = "MakeRangeEndExclusive expr=$exprDebug"
}