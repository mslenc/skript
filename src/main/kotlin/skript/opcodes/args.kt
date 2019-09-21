package skript.opcodes

import skript.exec.RuntimeState

class ArgsExtractRegular(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractArg(name)
        }
    }
    override fun toString() = "ArgsExtractRegular name=$name localIndex=$localIndex"
}

class ArgsExtractPosVarArgs(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractPosVarArgs(name)
        }
    }
    override fun toString() = "ArgsExtractPosVarArgs name=$name localIndex=$localIndex"
}

class ArgsExtractKwOnly(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractKwOnlyArg(name)
        }
    }
    override fun toString() = "ArgsExtractKwOnly name=$name localIndex=$localIndex"
}

class ArgsExtractKwVarArgs(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractKwVarArgs(name)
        }
    }
    override fun toString() = "ArgsExtractKwVarArgs name=$name localIndex=$localIndex"
}

object ArgsExpectNothingElse : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            args.expectNothingElse()
        }
    }
    override fun toString() = "ArgsExpectNothingElse"
}