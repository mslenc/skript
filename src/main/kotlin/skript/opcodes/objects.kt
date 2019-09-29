package skript.opcodes

import skript.exec.FunctionDef
import skript.exec.RuntimeState
import skript.parser.Pos
import skript.typeError
import skript.values.SkBoolean
import skript.values.SkClass
import skript.values.SkScriptFunction

class MakeFunction(val def: FunctionDef) : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
        state.topFrame.apply {
            val closure = makeClosure(def.framesCaptured)
            stack.push(SkScriptFunction(def, closure))
        }
        return null
    }

    override fun toString() = "MakeFunction def=$def"
}

object ObjectIsOp : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
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
        return null
    }

    override fun toString() = "ObjectIsOp"
}

object ObjectIsntOp : FastOpCode() {
    override fun execute(state: RuntimeState): OpCodeResult? {
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
        return null
    }

    override fun toString() = "ObjectIsntOp"
}

object ValueInOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val container = pop()
            val value = pop()

            push(SkBoolean.valueOf(container.contains(value, state)))
        }
        return null
    }

    override fun toString() = "ValueInOp"
}

object ValueNotInOp : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState): OpCodeResult? {
        state.topFrame.stack.apply {
            val container = pop()
            val value = pop()

            push(SkBoolean.valueOf(!container.contains(value, state)))
        }
        return null
    }

    override fun toString() = "ValueNotInOp"
}