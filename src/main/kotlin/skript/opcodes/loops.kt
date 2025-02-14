package skript.opcodes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
        return this // TODO: should we try to produce Iterator?
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        throw UnsupportedOperationException("Iterators can't be converted to JSON.")
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

data object MakeIterator: SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val container = pop()
            val iterator = container.makeIterator() ?: typeError("Can't iterate over " + container.asString().value, Pos(0, 0, "TODO"))
            push(iterator)
        }
        return null
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
}