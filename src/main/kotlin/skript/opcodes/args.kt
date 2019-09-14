package skript.opcodes

import skript.exec.RuntimeState

class ArgsExtractRegular(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractArg(name)
        }
    }
}

class ArgsExtractPosVarArgs(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractPosVarArgs(name)
        }
    }
}

class ArgsExtractKwOnly(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractKwOnlyArg(name)
        }
    }
}

class ArgsExtractKwVarArgs(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.extractKwVarArgs(name)
        }
    }
}

object ArgsExpectNothingElse : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            args.expectNothingElse()
        }
    }
}