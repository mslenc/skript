package skript.opcodes

import skript.analysis.StackSizeInfoReceiver
import skript.exec.Frame
import skript.values.SkValue

sealed class OpCode {
    abstract fun execute(frame: Frame): OpCodeResult?
    abstract suspend fun executeSuspend(frame: Frame): OpCodeResult?

    abstract fun getStackInfo(receiver: StackSizeInfoReceiver)
}

abstract class FastOpCode : OpCode() {
    final override suspend fun executeSuspend(frame: Frame): OpCodeResult {
        throw IllegalStateException("executeSuspend() should never be called on FastOpCodes")
    }
}

abstract class SuspendOpCode : OpCode()  {
    final override fun execute(frame: Frame) = ExecuteSuspend
}

sealed class OpCodeResult

object ExecuteSuspend : OpCodeResult()

class JumpTarget : OpCodeResult() {
    var value: Int = -1
}

class ReturnValue(val result: SkValue) : OpCodeResult()

class ThrowException(val ex: Throwable) : OpCodeResult()
