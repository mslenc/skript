package skript.opcodes

import skript.exec.RuntimeState
import skript.notSupported
import skript.values.*

class PushLiteral(val literal: SkValue) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(literal)
        }
    }
}

object MapNew : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(SkMap())
        }
    }
}

class MapDupSetKnownKey(private val key: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val map = stack.top() as SkMap
            map.setMapMember(key, value)
        }
    }
}

object MapDupSetKey : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val map = stack.top() as SkMap
            map.setMemberInternal(key, value)
        }
    }
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
                throw notSupported("Only other maps can be spread into map literals")
            }
        }
    }
}

object ListNew : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(SkList())
        }
    }
}

object ListDupAppend : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val value = stack.pop()
            val list = stack.top() as SkList
            list.push(value)
        }
    }
}

class ListDupAppendLiteral(private val literal: SkValue) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val list = stack.top() as SkList
            list.setSlot(list.getLength(), literal)
        }
    }
}

object ListDupAppendSpread : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val newElements = stack.pop()
            val list = stack.top() as SkList

            if (newElements is SkList) {
                list.pushAll(newElements)
            } else {
                throw notSupported("Can't spread a non-list into a list")
            }
        }
    }
}

object StringTemplateStart : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.push(SkStringBuilder())
    }
}

val StringTemplateEnd = ConvertToString

class StringTemplateAppendRaw(val text: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        val sb = state.topFrame.stack.top() as SkStringBuilder
        sb.appendRawText(text)
    }
}

object StringTemplateAppend : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val value = pop()
            val sb = top() as SkStringBuilder
            sb.append(value)
        }
    }
}