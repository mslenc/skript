package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame

abstract class ArgsFastOpCode : FastOpCode() {
    final override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(0)
    }
}

class ArgsExtractRegular(val name: String, val localIndex: Int): ArgsFastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            locals[localIndex] = args.extractArg(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractRegular name=$name localIndex=$localIndex"
}

class ArgsExtractPosVarArgs(val name: String, val localIndex: Int): ArgsFastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            locals[localIndex] = args.extractPosVarArgs(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractPosVarArgs name=$name localIndex=$localIndex"
}

class ArgsExtractKwOnly(val name: String, val localIndex: Int): ArgsFastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            locals[localIndex] = args.extractKwOnlyArg(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractKwOnly name=$name localIndex=$localIndex"
}

class ArgsExtractKwVarArgs(val name: String, val localIndex: Int): ArgsFastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            locals[localIndex] = args.extractKwVarArgs(name)
        }
        return null
    }
    override fun toString() = "ArgsExtractKwVarArgs name=$name localIndex=$localIndex"
}

object ArgsExpectNothingElse : ArgsFastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            args.expectNothingElse()
        }
        return null
    }
    override fun toString() = "ArgsExpectNothingElse"
}