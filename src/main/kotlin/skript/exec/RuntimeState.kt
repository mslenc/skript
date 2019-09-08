package skript.exec

import skript.exec.Frame.Companion.EMPTY_ARRAY
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.util.Globals
import skript.util.Stack
import skript.values.SkNull
import skript.values.SkValue

val dummyFrame = Frame(0, emptyArray(), EMPTY_ARRAY, emptyList(), emptyMap())

class RuntimeState(val globals: Globals, val env: SkriptEnv) {
    var topFrame: Frame = dummyFrame
    val otherFrames = Stack<Frame>()

    suspend fun executeFunction(func: FunctionDef, closure: Array<Frame>, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val ops = func.ops
        val opsSize = ops.size

        val frame = Frame(func.localsSize, ops, closure, posArgs, kwArgs)

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

            return frame.result
        } finally {
            topFrame = otherFrames.pop()
        }
    }

    fun importKotlinValue(value: Any?): SkValue {
        if (value == null) return SkNull

        if (value is String) return value.toSkript()

        TODO("Can't import ${ value.javaClass }")
    }
}