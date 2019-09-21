package skript.opcodes

import skript.exec.RuntimeState
import skript.typeError
import skript.util.SkArguments
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
            argsStack.push(SkArguments())
        }
    }
    override fun toString() = "BeginArgs"
}

object AddPosArg : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            argsStack.top().addPosArg(stack.pop())
        }
    }
    override fun toString() = "AddPosArg"
}

class AddKwArg(private val name: String) : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            argsStack.top().addKwArg(name, stack.pop())
        }
    }
    override fun toString() = "AddKwArg name=$name"
}

object SpreadPosArgs : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val arr = stack.pop()

            if (arr is SkAbstractList) {
                argsStack.top().spreadPosArgs(arr)
            } else {
                typeError("Only lists can be used for spreading arguments")
            }
        }
    }
    override fun toString() = "SpreadPosArgs"
}

object SpreadKwArgs : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            val kws = stack.pop()

            if (kws is SkMap) {
                argsStack.top().spreadKwArgs(kws)
            } else {
                typeError("Only maps can be used for spreading arguments")
            }
        }
    }
    override fun toString() = "SpreadKwArgs"
}

class CallMethod(val name: String, val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val thiz = stack.pop()
            val args = argsStack.pop()
            val result = thiz.callMethod(name, args, state, exprDebug)
            stack.push(result)
        }
    }
    override fun toString() = "CallMethod name=$name expr=$exprDebug"
}

class CallFunction(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(state: RuntimeState) {
        state.topFrame.apply {
            val func = stack.pop()
            val args = argsStack.pop()

            if (func is SkFunction || func is SkClass) {
                val result = func.call(args, state)
                stack.push(result)
            } else {
                typeError("$exprDebug is not a function or a class")
            }
        }
    }
    override fun toString() = "CallFunction expr=$exprDebug"
}

object Return : FastOpCode() {
    override fun execute(state: RuntimeState) {
        state.topFrame.apply {
            result = stack.pop()
            ip = ops.size
        }
    }
    override fun toString() = "Return"
}