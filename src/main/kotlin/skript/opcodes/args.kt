package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkList
import skript.values.SkMap

class ArgsExtractNamed(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = args.getParam(name)
        }
    }
}

class ArgsExtractRemainingPos(val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = SkList(args.getRemainingPosArgs())
        }
    }
}

class ArgsExtractRemainingKw(val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            locals[localIndex] = SkMap(args.getRemainingKwArgs())
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