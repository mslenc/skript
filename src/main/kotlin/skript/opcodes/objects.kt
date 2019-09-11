package skript.opcodes

import skript.exec.Frame
import skript.exec.FunctionDef
import skript.exec.RuntimeState
import skript.parser.Pos
import skript.typeError
import skript.values.SkBoolean
import skript.values.SkClass
import skript.values.SkScriptFunction

class MakeFunction(val def: FunctionDef) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val closure: Array<Frame> = when (def.framesCaptured) {
                0 -> Frame.EMPTY_ARRAY
                1 -> arrayOf(this)
                else -> Array(def.framesCaptured) {
                    when (it) {
                        0 -> this
                        else -> this.closure[it - 1]
                    }
                }
            }

            stack.push(SkScriptFunction(def, closure))
        }
    }
}

object ObjectIsOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val klass = pop()
            val value = pop()

            val result = if (klass is SkClass) {
                SkBoolean.valueOf(klass.isInstance(value))
            } else {
                typeError("Right-hand side of 'is' is not a class")
            }

            push(result)
        }
    }
}

object ObjectIsntOp : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.stack.apply {
            val klass = pop()
            val value = pop()

            val result = if (klass is SkClass) {
                SkBoolean.valueOf(!klass.isInstance(value))
            } else {
                typeError("Right-hand side of '!is' is not a class", Pos(0, 0, "TODO"))
            }

            push(result)
        }
    }
}

object ValueInOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.stack.apply {
            val container = pop()
            val value = pop()

            push(SkBoolean.valueOf(container.hasOwnMember(value, state)))
        }
    }
}

object ValueNotInOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.stack.apply {
            val container = pop()
            val value = pop()

            push(SkBoolean.valueOf(!container.hasOwnMember(value, state)))
        }
    }
}