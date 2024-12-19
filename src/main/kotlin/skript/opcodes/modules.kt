package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.io.ModuleName
import skript.parser.Pos

class GetModuleExports(val moduleName: ModuleName) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.push(frame.env.getModuleExports(moduleName))
        return null
    }

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}

class RequireModuleExports(val sourceName: String, val importingModuleName: ModuleName, val pos: Pos) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.push(frame.env.requireModule(sourceName, importingModuleName, pos).exports)
        return null
    }

    override fun getStackInfo(receiver: StackSizeInfoReceiver) {
        receiver.normalCase(1)
    }
}