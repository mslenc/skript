package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.util.OpCache

class GetLocal private constructor(private val varIndex: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(locals[varIndex])
        }
        return null
    }

    override fun toString() = "GetLocal varIndex=$varIndex"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }

    companion object {
        private val cache = OpCache.createOpCache(20) { GetLocal(it) }

        operator fun invoke(varIndex: Int) = cache(varIndex)
    }
}

class SetLocal private constructor(private val varIndex: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            locals[varIndex] = stack.pop()
        }
        return null
    }

    override fun toString() = "SetLocal varIndex=$varIndex"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }

    companion object {
        private val cache = OpCache.createOpCache(20) { SetLocal(it) }

        operator fun invoke(varIndex: Int) = cache(varIndex)
    }
}

class GetClosureVar(private val closureIndex: Int, private val varIndex: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            stack.push(closure[closureIndex][varIndex])
        }
        return null
    }

    override fun toString() = "GetClosureVar closureIndex=$closureIndex varIndex=$varIndex"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

class SetClosureVar(private val closureIndex: Int, private val varIndex: Int) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            closure[closureIndex][varIndex] = stack.pop()
        }
        return null
    }

    override fun toString() = "SetClosureVar closureIndex=$closureIndex varIndex=$varIndex"

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(-1)
    }
}
