package skript.exec

import skript.opcodes.OpCode
import skript.util.SkArguments
import skript.util.Stack
import skript.values.SkUndefined
import skript.values.SkValue

class Frame(localsSize: Int, val ops: Array<OpCode>, val args: SkArguments, val closure: Array<Array<SkValue>> = EMPTY_CLOSURE) {
    val locals = Array<SkValue>(localsSize) { SkUndefined }
    val stack = Stack<SkValue>()
    val argsStack = Stack<SkArguments>()
    var ip = 0
    var result: SkValue = SkUndefined

    fun makeClosure(size: Int): Array<Array<SkValue>> {
        return when(size) {
            0 -> EMPTY_CLOSURE
            1 -> arrayOf(locals)
            else -> {
                Array(size) {
                    when (it) {
                        0 -> locals
                        else -> closure[it - 1]
                    }
                }
            }
        }
    }

    companion object {
        val EMPTY_CLOSURE = arrayOf<Array<SkValue>>()
    }
}