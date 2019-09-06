package skript.opcodes

import skript.exec.RuntimeState
import skript.util.ArgsExtractor
import skript.values.SkList
import skript.values.SkMap

class MakeArgsConstructor(val funcName: String): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val args = ArgsExtractor(posArgs, kwArgs, funcName)
            stack.push(args)
        }
    }
}

class ArgsExtractNamed(val name: String, val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val args = stack.top() as ArgsExtractor
            val value = args.extractParam(name)
            locals[localIndex] = value
        }
    }
}

class ArgsExtractRemainingPos(val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val args = stack.top() as ArgsExtractor
            val values = args.getRemainingPosArgs()
            locals[localIndex] = SkList(values)
        }
    }
}

class ArgsExtractRemainingKw(val localIndex: Int): FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val args = stack.top() as ArgsExtractor
            val values = args.getRemainingKwArgs()
            locals[localIndex] = SkMap(values)
        }
    }
}

object ArgsExpectNothingElse : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val args = stack.top() as ArgsExtractor
            args.expectNothingElse()
        }
    }
}