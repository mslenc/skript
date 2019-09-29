package skript.exec

import skript.io.SkriptEnv
import skript.opcodes.*
import skript.util.Globals
import skript.util.SkArguments
import skript.util.Stack
import skript.values.SkValue

val dummyFrame = Frame(0, emptyArray(), SkArguments())

class RuntimeState(val env: SkriptEnv) {
    val globals: Globals = env.globals
    var topFrame: Frame = dummyFrame
    val otherFrames = Stack<Frame>()

    suspend fun executeFunction(func: FunctionDef, closure: Array<Array<SkValue>>, args: SkArguments): SkValue {
        val ops = func.ops
        val opsSize = ops.size

        val frame = Frame(func.localsSize, ops, args, closure)

        otherFrames.push(topFrame)
        try {
            topFrame = frame

            nextOp@
            while (frame.ip < opsSize) {
                val op = ops[frame.ip++]
                var result = op.execute(this)
                if (result === ExecuteSuspend)
                    result = op.executeSuspend(this)

                when (result) {
                    null -> continue@nextOp
                    is JumpTarget -> frame.ip = result.value
                    is ReturnValue -> return result.result
                    is ThrowException -> throw result.ex
                }
            }

            check(topFrame.stack.size == 0)

            return frame.result
        } finally {
            topFrame = otherFrames.pop()
        }
    }
}