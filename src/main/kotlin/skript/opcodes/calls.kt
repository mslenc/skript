package skript.opcodes

import skript.exec.Frame
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



data object BeginArgs : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            argsStack.push(SkArguments())
        }
        return null
    }
}

data object AddPosArg : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            argsStack.top().addPosArg(stack.pop())
        }
        return null
    }
}

class AddKwArg(private val name: String) : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            argsStack.top().addKwArg(name, stack.pop())
        }
        return null
    }
    override fun toString() = "AddKwArg name=$name"
}

data object SpreadPosArgs : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val arr = stack.pop()

            if (arr is SkAbstractList) {
                argsStack.top().spreadPosArgs(arr)
            } else {
                typeError("Only lists can be used for spreading arguments")
            }
        }
        return null
    }
}

data object SpreadKwArgs : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult? {
        frame.apply {
            val kws = stack.pop()

            if (kws is SkMap) {
                argsStack.top().spreadKwArgs(kws)
            } else {
                typeError("Only maps can be used for spreading arguments")
            }
        }
        return null
    }
}

class CallMethod(val name: String, val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val thiz = stack.pop()
            val args = argsStack.pop()
            val result = thiz.callMethod(name, args, frame.env, exprDebug)
            stack.push(result)
        }
        return null
    }
    override fun toString() = "CallMethod name=$name expr=$exprDebug"
}

class CallFunction(val exprDebug: String) : SuspendOpCode() {
    override suspend fun executeSuspend(frame: Frame): OpCodeResult? {
        frame.apply {
            val func = stack.pop()
            val args = argsStack.pop()

            if (func is SkFunction || func is SkClass) {
                val result = func.call(args, frame.env)
                stack.push(result)
            } else {
                typeError("$exprDebug is not a function or a class (it is ${ func.javaClass })")
            }
        }
        return null
    }
    override fun toString() = "CallFunction expr=$exprDebug"
}

data object Return : FastOpCode() {
    override fun execute(frame: Frame): OpCodeResult {
        return ReturnValue(frame.stack.pop())
    }
}