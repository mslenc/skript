package skript.opcodes

import skript.exec.RuntimeState

sealed class OpCode {
    abstract val isSuspend: Boolean

    abstract fun execute(state: RuntimeState)
    abstract suspend fun executeSuspend(state: RuntimeState)
}

abstract class FastOpCode : OpCode() {
    final override val isSuspend: Boolean
        get() = false

    final override suspend fun executeSuspend(state: RuntimeState) {
        throw IllegalStateException("executeSuspend() should never be called on FastOpCodes")
    }
}

abstract class SuspendOpCode : OpCode()  {
    final override val isSuspend: Boolean
        get() = true

    final override fun execute(state: RuntimeState) {
        throw IllegalStateException("execute() should never be called on SuspendOpCodes")
    }
}