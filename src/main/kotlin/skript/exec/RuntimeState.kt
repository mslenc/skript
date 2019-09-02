package skript.exec

import skript.opcodes.OpCode
import skript.util.Globals
import skript.util.Stack
import skript.values.SkValue

class RuntimeState {
    val frames = Stack<Frame>()
    val globals = Globals()

    val topFrame get() = frames.top()

    fun startScriptFrame(ops: Array<OpCode>, localsSize: Int, closure: Array<Frame>): Frame {
        val frame = Frame(localsSize, ops, closure)
        frames.push(frame)
        return frame
    }

    suspend fun execute(): SkValue {
        val frame = topFrame
        val ops = frame.ops
        val opsSize = ops.size

        while (frame.ip < opsSize) {
            val op = ops[frame.ip++]
            if (op.isSuspend) {
                op.executeSuspend(this)
            } else {
                op.execute(this)
            }
        }

        return frames.pop().result
    }
}