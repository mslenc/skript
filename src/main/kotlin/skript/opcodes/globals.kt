package skript.opcodes

import skript.exec.Frame
import skript.values.SkMap
import skript.values.SkUndefined

class GetGlobal(private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.push(frame.env.globals.get(name))
        return null
    }
    override fun toString() = "GetGlobal name=$name"
}

class SetGlobal(private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.env.globals.set(name, frame.stack.pop())
        return null
    }
    override fun toString() = "SetGlobal name=$name"
}


class GetCtxOrGlobal(private val getCtx: FastOpCode, private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        getCtx.execute(frame)
        val ctx = frame.stack.pop()
        if (ctx is SkMap) {
            ctx.entries[name]?.let {
                if (it != SkUndefined) {
                    frame.stack.push(it)
                    return null
                }
            }
        }

        frame.stack.push(frame.env.globals.get(name))
        return null
    }
    override fun toString() = "GetCtxOrGlobal name=$name"
}

class SetCtxOrGlobal(private val getCtx: FastOpCode, private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        val value = frame.stack.pop()
        getCtx.execute(frame)
        val ctx = frame.stack.pop()
        if (ctx is SkMap) {
            if (ctx.entries.containsKey(name)) {
                ctx.entries[name] = value
                return null
            }
        }

        frame.env.globals.set(name, value)
        return null
    }
    override fun toString() = "SetCtxOrGlobal name=$name"
}