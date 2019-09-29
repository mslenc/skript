package skript.opcodes

import skript.exec.RuntimeState
import skript.values.SkValue

sealed class OpCode {
    abstract fun execute(state: RuntimeState): OpCodeResult?
    abstract suspend fun executeSuspend(state: RuntimeState): OpCodeResult?
}

abstract class FastOpCode : OpCode() {
    final override suspend fun executeSuspend(state: RuntimeState): OpCodeResult {
        throw IllegalStateException("executeSuspend() should never be called on FastOpCodes")
    }
}

abstract class SuspendOpCode : OpCode()  {
    final override fun execute(state: RuntimeState) = ExecuteSuspend
}

sealed class OpCodeResult

object ExecuteSuspend : OpCodeResult()

class JumpTarget : OpCodeResult() {
    var value: Int = -1
}

class ReturnValue(val result: SkValue) : OpCodeResult()

class ThrowException(val ex: Throwable) : OpCodeResult()
