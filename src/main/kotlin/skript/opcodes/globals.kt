package skript.opcodes

import skript.exec.RuntimeState

class GetGlobal(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.push(state.globals.get(name))
    }
    override fun toString() = "GetGlobal name=$name"
}

class SetGlobal(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.globals.set(name, state.topFrame.stack.pop())
    }
    override fun toString() = "SetGlobal name=$name"
}

class GetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        val vars = state.env.modules[moduleName]?.vars ?: throw IllegalStateException("No module $moduleName in runtime")
        state.topFrame.stack.push(vars[indexInModule])
    }
    override fun toString() = "GetModuleVar moduleName=$moduleName indexInModule=$indexInModule"
}

class SetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        val vars = state.env.modules[moduleName]?.vars ?: throw IllegalStateException("No module $moduleName in runtime")
        vars[indexInModule] = state.topFrame.stack.pop()
    }
    override fun toString() = "SetModuleVar moduleName=$moduleName indexInModule=$indexInModule"
}