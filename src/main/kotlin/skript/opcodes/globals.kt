package skript.opcodes

import skript.exec.RuntimeState

class GetGlobal(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.frames.top().stack.push(state.globals.get(name))
    }
}

class SetGlobal(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.globals.set(name, state.frames.top().stack.pop())
    }
}

class GetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class SetModuleVar(private val moduleName: String, private val indexInModule: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}