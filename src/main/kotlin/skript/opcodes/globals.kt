package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.io.ModuleName
import skript.io.toSkript
import skript.values.SkMap
import skript.values.SkUndefined

class GetGlobal(private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.push(frame.env.globals.get(name))
        return null
    }
    override fun toString() = "GetGlobal name=$name"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

class SetGlobal(private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.env.globals.set(name, frame.stack.pop())
        return null
    }
    override fun toString() = "SetGlobal name=$name"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}

class GetModuleVar(private val moduleName: ModuleName, private val indexInModule: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        val vars = frame.env.getModuleVars(moduleName)
        frame.stack.push(vars[indexInModule])
        return null
    }
    override fun toString() = "GetModuleVar moduleName=$moduleName indexInModule=$indexInModule"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

class SetModuleVar(private val moduleName: ModuleName, private val indexInModule: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        val vars = frame.env.modules[moduleName]?.moduleVars ?: throw IllegalStateException("No module $moduleName in runtime")
        vars[indexInModule] = frame.stack.pop()
        return null
    }
    override fun toString() = "SetModuleVar moduleName=$moduleName indexInModule=$indexInModule"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
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

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}