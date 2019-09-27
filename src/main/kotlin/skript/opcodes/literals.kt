package skript.opcodes

import skript.exec.RuntimeState
import skript.typeError
import skript.values.*

class PushLiteral(val literal: SkValue) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(literal)
        }
    }
    override fun toString() = "PushLiteral literal=$literal"
}

object MapNew : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(SkMap())
        }
    }
    override fun toString() = "MapNew"
}

class MapDupSetKnownKey(private val key: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val map = stack.top() as SkMap
            map.entries[key] = value
        }
    }
    override fun toString() = "MapDupSetKnownKey key=$key"
}

object MapDupSetKey : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val map = stack.top() as SkMap
            map.entries[key.asString().value] = value
        }
    }
    override fun toString() = "MapDupSetKey"
}

object MapDupSpreadValues : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val values = stack.pop()

            if (values == SkNull || values == SkUndefined)
                return

            if (values is SkMap) {
                val map = stack.top() as SkMap
                map.spreadFrom(values)
            } else {
                typeError("Only other maps can be spread into map literals")
            }
        }
    }
    override fun toString() = "MapDupSpreadValues"
}

object ListNew : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(SkList())
        }
    }
    override fun toString() = "ListNew"
}

object ListDupAppend : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val list = stack.top() as SkList
            list.add(value)
        }
    }
    override fun toString() = "ListDupAppend"
}

class ListDupAppendLiteral(private val literal: SkValue) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val list = stack.top() as SkList
            list.setSlot(list.getSize(), literal)
        }
    }
    override fun toString() = "ListDupAppendLiteral literal=$literal"
}

object ListDupAppendSpread : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val newElements = stack.pop()
            val list = stack.top() as SkList

            when (newElements) {
                is SkList -> list.addAll(newElements.listEls)
                is SkAbstractList -> list.addAll(newElements)
                else -> typeError("Can't spread a non-list into a list")
            }
        }
    }
    override fun toString() = "ListDupAppendSpread"
}

object StringTemplateStart : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.push(SkStringBuilder())
    }
    override fun toString() = "StringTemplateStart"
}

val StringTemplateEnd = ConvertToString

class StringTemplateAppendRaw(val text: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        val sb = state.topFrame.stack.top() as SkStringBuilder
        sb.appendRawText(text)
    }
    override fun toString() = "StringTemplateAppendRaw text=$text"
}

object StringTemplateAppend : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val value = pop()
            val sb = top() as SkStringBuilder
            sb.append(value)
        }
    }
    override fun toString() = "StringTemplateAppend"
}