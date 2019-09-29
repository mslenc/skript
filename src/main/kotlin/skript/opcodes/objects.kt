package skript.opcodes

import skript.exec.Frame
import skript.exec.FunctionDef
import skript.parser.Pos
import skript.typeError
import skript.values.SkBoolean
import skript.values.SkClass
import skript.values.SkScriptFunction

class MakeFunction(val def: FunctionDef) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val closure = makeClosure(def.framesCaptured)
            stack.push(SkScriptFunction(def, closure))
        }
        return null
    }

    override fun toString() = "MakeFunction def=$def"
}

object ObjectIsOp : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
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
    override fun execute(frame: Frame): OpCodeResult? {
        frame.stack.apply {
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
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val container = pop()
            val value = pop()

            push(SkBoolean.valueOf(container.contains(value, frame.env)))
        }
        return null
    }

    override fun toString() = "ValueInOp"
}

object ValueNotInOp : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.stack.apply {
            val container = pop()
            val value = pop()

            push(SkBoolean.valueOf(!container.contains(value, frame.env)))
        }
        return null
    }

    override fun toString() = "ValueNotInOp"
}