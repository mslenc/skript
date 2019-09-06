package skript.opcodes

import skript.exec.RuntimeState

class GetGlobal(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.push(state.globals.get(name))
    }
}

class SetGlobal(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.globals.set(name, state.topFrame.stack.pop())
    }
}

class GetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        val vars = state.env.modules[moduleName]?.vars ?: throw IllegalStateException("No module $moduleName in runtime")
        state.topFrame.stack.push(vars[indexInModule])
    }
}

class SetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        val vars = state.env.modules[moduleName]?.vars ?: throw IllegalStateException("No module $moduleName in runtime")
        vars[indexInModule] = state.topFrame.stack.pop()
    }
}