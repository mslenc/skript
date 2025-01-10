package skript.opcodes

import skript.exec.Frame

abstract class ArgsFastOpCode : FastOpCode()

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

data object ArgsExpectNothingElse : ArgsFastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            args.expectNothingElse()
        }
        return null
    }
}