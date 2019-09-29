package skript.analysis

import skript.opcodes.JumpTarget

class StackSizeAnalyzer {

}

class StackSizeInfoReceiver {
    fun normalCase(stackSizeDelta: Int, argsStackSizeDelta: Int = 0): Unit = TODO()
    fun jumpCase(stackSizeDelta: Int, target: JumpTarget, argsStackSizeDelta: Int = 0): Unit = TODO()
}