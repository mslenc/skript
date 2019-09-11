package skript.exec

import skript.opcodes.OpCode
import skript.util.SkArguments
import skript.util.Stack
import skript.values.SkUndefined
import skript.values.SkValue

class Frame(localsSize: Int, val ops: Array<OpCode>, val closure: Array<Frame> = EMPTY_ARRAY, val args: SkArguments) {
    val locals = Array<SkValue>(localsSize) { SkUndefined }
    val stack = Stack<SkValue>()
    val argsStack = Stack<SkArguments>()
    var ip = 0
    var result: SkValue = SkUndefined

    companion object {
        val EMPTY_ARRAY = arrayOf<Frame>()
    }
}