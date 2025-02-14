package skript.opcodes

import skript.exec.Frame
import skript.typeError
import skript.values.*

class PushLiteral(val literal: SkValue) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(literal)
        }
        return null
    }
    override fun toString() = "PushLiteral literal=$literal"
}

data object MapNew : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(SkMap())
        }
        return null
    }
}

class MapDupSetKnownKey(private val key: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val map = stack.top() as SkMap
            map.entries[key] = value
        }
        return null
    }
    override fun toString() = "MapDupSetKnownKey key=$key"
}

data object MapDupSetKey : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val key = stack.pop()
            val map = stack.top() as SkMap
            map.entries[key.asString().value] = value
        }
        return null
    }
}

data object MapDupSpreadValues : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val values = stack.pop()

            if (values == SkNull || values == SkUndefined)
                return null

            if (values is SkMap) {
                val map = stack.top() as SkMap
                map.spreadFrom(values)
            } else {
                typeError("Only other maps can be spread into map literals")
            }
        }
        return null
    }
}

data object ListNew : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(SkList())
        }
        return null
    }
}

data object ListDupAppend : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val value = stack.pop()
            val list = stack.top() as SkList
            list.add(value)
        }
        return null
    }
}

class ListDupAppendLiteral(private val literal: SkValue) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val list = stack.top() as SkList
            list.setSlot(list.getSize(), literal)
        }
        return null
    }
    override fun toString() = "ListDupAppendLiteral literal=$literal"
}

data object ListDupAppendSpread : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val newElements = stack.pop()
            val list = stack.top() as SkList

            when (newElements) {
                is SkList -> list.addAll(newElements.listEls)
                is SkAbstractList -> list.addAll(newElements)
                else -> typeError("Can't spread a non-list into a list")
            }
        }
        return null
    }
}

data object StringTemplateStart : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.push(SkStringBuilder())
        return null
    }
}

val StringTemplateEnd = ConvertToString

class StringTemplateAppendRaw(val text: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        val sb = frame.stack.top() as SkStringBuilder
        sb.appendRawText(text)
        return null
    }
    override fun toString() = "StringTemplateAppendRaw text=$text"
}

data object StringTemplateAppend : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val value = pop()
            val sb = top() as SkStringBuilder
            sb.append(value)
        }
        return null
    }
}