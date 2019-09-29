package skript.opcodes

import skript.exec.Frame

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

class GetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        val vars = frame.env.modules[moduleName]?.vars ?: throw IllegalStateException("No module $moduleName in runtime")
        frame.stack.push(vars[indexInModule])
        return null
    }
    override fun toString() = "GetModuleVar moduleName=$moduleName indexInModule=$indexInModule"
}

class SetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        val vars = frame.env.modules[moduleName]?.vars ?: throw IllegalStateException("No module $moduleName in runtime")
        vars[indexInModule] = frame.stack.pop()
        return null
    }
    override fun toString() = "SetModuleVar moduleName=$moduleName indexInModule=$indexInModule"
}