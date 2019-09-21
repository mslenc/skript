package skript.exec

import skript.exec.Frame.Companion.EMPTY_ARRAY
import skript.io.SkriptEnv
import skript.util.Globals
import skript.util.SkArguments
import skript.util.Stack
import skript.values.SkValue

val dummyFrame = Frame(0, emptyArray(), EMPTY_ARRAY, SkArguments())

class RuntimeState(val env: SkriptEnv) {
    val globals: Globals = env.globals
    var topFrame: Frame = dummyFrame
    val otherFrames = Stack<Frame>()

    suspend fun executeFunction(func: FunctionDef, closure: Array<Frame>, args: SkArguments): SkValue {
        val ops = func.ops
        val opsSize = ops.size

        val frame = Frame(func.localsSize, ops, closure, args)

        otherFrames.push(topFrame)
        try {
            topFrame = frame

            while (frame.ip < opsSize) {
                val op = ops[frame.ip++]
                if (op.isSuspend) {
                    op.executeSuspend(this)
                } else {
                    op.execute(this)
                }
            }

            check(topFrame.stack.size == 0)

            return frame.result
        } finally {
            topFrame = otherFrames.pop()
        }
    }
}