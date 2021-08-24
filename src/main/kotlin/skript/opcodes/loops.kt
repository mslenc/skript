package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.io.toSkript
import skript.parser.Pos
import skript.typeError
import skript.values.*

abstract class SkIterator : SkObject() {
    abstract fun moveToNext(): Boolean
    abstract fun getCurrentKey(): SkValue
    abstract fun getCurrentValue(): SkValue

    override fun unwrap(): SkIterator {
        return this // TODO: should we try to produce
    }
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
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val container = pop()
            val iterator = container.makeIterator() ?: typeError("Can't iterate over " + container.asString().value, Pos(0, 0, "TODO"))
            push(iterator)
        }
        return null
    }
    override fun toString() = "MakeIterator"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(0)
    }
}

class IteratorNext(val pushKey: Boolean, val pushValue: Boolean, val end: JumpTarget) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val iterator = stack.top() as SkIterator

            if (iterator.moveToNext()) {
                if (pushKey)
                    stack.push(iterator.getCurrentKey())
                if (pushValue)
                    stack.push(iterator.getCurrentValue())
            } else {
                stack.pop()
                return end
            }
        }
        return null
    }
    override fun toString() = "IteratorNext end=${end.value} pushKey=${pushKey} pushValue=${pushValue}"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase((if (pushKey) 1 else 0) + (if (pushValue) 1 else 0))
        receiver.jumpCase(-1, end)
    }
}

class MakeRangeEndInclusive(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val to = pop()
            val from = pop()
            push(from.makeRange(to, true, frame.env, exprDebug))
        }
        return null
    }
    override fun toString() = "MakeRangeEndInclusive expr=$exprDebug"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}

class MakeRangeEndExclusive(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val to = pop()
            val from = pop()
            push(from.makeRange(to, false, frame.env, exprDebug))
        }
        return null
    }
    override fun toString() = "MakeRangeEndExclusive expr=$exprDebug"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}