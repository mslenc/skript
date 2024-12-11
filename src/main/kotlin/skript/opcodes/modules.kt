package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.parser.Pos

object CurrentModuleExports : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.push(frame.module.exports)
        return null
    }

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

class RequireModuleExports(val moduleName: String, val pos: Pos) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.push(frame.env.requireModule(moduleName, pos).exports)
        return null
    }

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}