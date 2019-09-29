package skript.opcodes

import skript.exec.RuntimeState

class ArgsExtractRegular(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            locals[localIndex] = args.extractArg(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractRegular name=$name localIndex=$localIndex"
}

class ArgsExtractPosVarArgs(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            locals[localIndex] = args.extractPosVarArgs(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractPosVarArgs name=$name localIndex=$localIndex"
}

class ArgsExtractKwOnly(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            locals[localIndex] = args.extractKwOnlyArg(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractKwOnly name=$name localIndex=$localIndex"
}

class ArgsExtractKwVarArgs(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            locals[localIndex] = args.extractKwVarArgs(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractKwVarArgs name=$name localIndex=$localIndex"
}

object ArgsExpectNothingElse : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            args.expectNothingElse()
        }
        return null
    }
    override fun toString() = "ArgsExpectNothingElse"
}