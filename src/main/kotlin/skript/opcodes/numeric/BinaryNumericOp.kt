package skript.opcodes.numeric

import skript.exec.RuntimeState
import skript.opcodes.FastOpCode
import skript.values.SkNumber
import skript.values.SkScalar

abstract class BinaryNumericOp : FastOpCode() {
    abstract fun computeResult(left: SkNumber, right: SkNumber): SkScalar

    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val right = stack.pop()
            val left = stack.pop()

            stack.push(computeResult(left.asNumber(), right.asNumber()))
        }
    }
}