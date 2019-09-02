package skript.opcodes

import skript.exec.Frame
import skript.exec.FunctionDef
import skript.exec.RuntimeState
import skript.notSupported
import skript.util.ArgsBuilder
import skript.values.SkFunction
import skript.values.SkList
import skript.values.SkMap
import skript.values.SkScriptFunction

/*
To call a method:

1) push the object whose method is to be called
2) BeginArgs
3) value + AddPosArg for each positional argument
4) value + AddKwArg(kw) for each keyword argument
5) list + SpreadPosArgs for spread positional arguments
6) map + SpreadKwArgs for spread keyword arguments
7) CallMethod(name)

To call a function:
1) push the function
2..6) are the same
7) CallFunction

 */



object BeginArgs : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            args.push(ArgsBuilder())
        }
    }
}

object AddPosArg : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            args.top().addPosArg(stack.pop())
        }
    }
}

class AddKwArg(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            args.top().addKwArg(name, stack.pop())
        }
    }
}

object SpreadPosArgs : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val arr = stack.pop()

            if (arr is SkList) {
                args.top().spreadPosArgs(arr)
            } else {
                notSupported("Only lists can be used for spreading arguments")
            }
        }
    }
}

object SpreadKwArgs : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val kws = stack.pop()

            if (kws is SkMap) {
                args.top().spreadKwArgs(kws)
            } else {
                notSupported("Only maps can be used for spreading arguments")
            }
        }
    }
}

class CallMethod(val name: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val thiz = stack.pop()
            val argsBuilder = args.pop()
            thiz.callMethod(name, argsBuilder.getPosArgs(), argsBuilder.getKwArgs(), state)
        }
    }
}

object CallFunction : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val func = stack.pop()
            val argsBuilder = args.pop()

            if (func is SkFunction) {
                func.call(argsBuilder.getPosArgs(), argsBuilder.getKwArgs(), state)
            } else {
                notSupported("Only functions can be called")
            }
        }
    }
}

object Return : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            result = stack.pop()
            ip = ops.size
        }
    }
}

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