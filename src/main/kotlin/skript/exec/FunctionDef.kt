package skript.exec

import skript.opcodes.OpCode
import skript.util.AstProps

enum class ParamType {
    NORMAL,
    POS_ARGS,
    KW_ARGS
}

class ParamDef(val name: String, val localIndex: Int, val type: ParamType)

class FunctionDef(val name: String, val paramDefs: Array<ParamDef>, val ops: Array<OpCode>, val localsSize: Int, val framesCaptured: Int) {
    init {
        assert(paramsWellBehaved(paramDefs, localsSize) == null) { paramsWellBehaved(paramDefs, localsSize)!! }
    }

    companion object : AstProps.Key<FunctionDef>
}

fun paramsWellBehaved(paramDefs: Array<ParamDef>, localsSize: Int): String? {
    val namesSeen = HashSet<String>()
    var state = 0 // 0 = normal, 1 = after *posArgs, 2 = after **kwArgs
    val indexesSeen = HashSet<Int>()

    for (param in paramDefs) {
        if (!namesSeen.add(param.name)) return "Parameter name ${param.name} repeats"
        if (!indexesSeen.add(param.localIndex)) return "Local index ${param.localIndex} repeats"
        if (param.localIndex < 0) return "localIndex is below zero"
        if (param.localIndex >= localsSize) return "localIndex is bigger than localsSize"

        when (param.type) {
            ParamType.NORMAL -> {
                if (state != 0) return "Normal parameter after catch parameters"
            }

            ParamType.POS_ARGS -> {
                when (state) {
                    0 -> { state = 1 }
                    1 -> return "*posArgs repeats more than once"
                    2 -> return "*posArgs after **kwArgs"
                }
            }

            ParamType.KW_ARGS -> {
                when (state) {
                    0 -> { state = 2 }
                    1 -> { state = 2 }
                    2 -> return "**kwArgs repeats more than once"
                }
            }
        }
    }

    return null
}
