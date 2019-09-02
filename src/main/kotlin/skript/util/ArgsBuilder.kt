package skript.util

import skript.values.SkList
import skript.values.SkMap
import skript.values.SkUndefined
import skript.values.SkValue

class ArgsBuilder {
    private lateinit var posArgs: ArrayList<SkValue>
    private lateinit var kwArgs: LinkedHashMap<String, SkValue>

    fun addPosArg(arg: SkValue) {
        if (!::posArgs.isInitialized)
            posArgs = ArrayList()

        posArgs.add(arg)
    }

    fun addKwArg(key: String, arg: SkValue) {
        if (!::kwArgs.isInitialized)
            kwArgs = LinkedHashMap()

        kwArgs[key] = arg
    }

    fun spreadPosArgs(args: SkList) {
        if (!::posArgs.isInitialized)
            posArgs = ArrayList()

        for (arg in args.elements)
            posArgs.add(arg ?: SkUndefined)
    }

    fun spreadKwArgs(args: SkMap) {
        if (!::kwArgs.isInitialized)
            kwArgs = LinkedHashMap()

        args.props.extractInto(kwArgs)
    }

    fun getPosArgs(): List<SkValue> {
        return when {
            ::posArgs.isInitialized -> posArgs
            else -> emptyList()
        }
    }

    fun getKwArgs(): Map<String, SkValue> {
        return when {
            ::kwArgs.isInitialized -> kwArgs
            else -> emptyMap()
        }
    }
}