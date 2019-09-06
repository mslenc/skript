package skript.opcodes

import skript.exec.RuntimeState
import skript.notSupported
import skript.typeError
import skript.util.ArgsBuilder
import skript.values.*

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
            val result = thiz.callMethod(name, argsBuilder.getPosArgs(), argsBuilder.getKwArgs(), state)
            stack.push(result)
        }
    }
}

class CallFunction(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val func = stack.pop()
            val argsBuilder = args.pop()

            if (func is SkFunction) {
                val result = func.call(argsBuilder.getPosArgs(), argsBuilder.getKwArgs(), state)
                stack.push(result)
            } else {
                typeError("$exprDebug is not a function")
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