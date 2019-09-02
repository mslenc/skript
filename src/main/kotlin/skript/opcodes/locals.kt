package skript.opcodes

import skript.exec.RuntimeState
import skript.util.OpCache

class GetLocal private constructor(private val varIndex: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(locals[varIndex])
        }
    }

    companion object {
        private val cache = OpCache.createOpCache(20) { GetLocal(it) }

        operator fun invoke(varIndex: Int) = cache(varIndex)
    }
}

class SetLocal private constructor(private val varIndex: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[varIndex] = stack.pop()
        }
    }

    companion object {
        private val cache = OpCache.createOpCache(20) { SetLocal(it) }

        operator fun invoke(varIndex: Int) = cache(varIndex)
    }
}

class GetClosureVar(private val closureIndex: Int, private val varIndex: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            stack.push(closure[closureIndex].locals[varIndex])
        }
    }
}

class SetClosureVar(private val closureIndex: Int, private val varIndex: Int) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            closure[closureIndex].locals[varIndex] = stack.pop()
        }
    }
}
