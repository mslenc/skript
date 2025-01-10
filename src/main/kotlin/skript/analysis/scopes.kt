package skript.analysis

import skript.opcodes.*
import java.lang.UnsupportedOperationException

enum class VarLocation {
    GLOBAL,
    LOCAL,
    CLOSURE,
    CTX_OR_GLOBAL
}

sealed class VarInfo(val name: String, val location: VarLocation) {
    abstract val loadOpCode: OpCode
    abstract val storeOpCode: OpCode
}

class GlobalVarInfo(name: String) : VarInfo(name, VarLocation.GLOBAL) {
    override val loadOpCode = GetGlobal(name)
    override val storeOpCode = SetGlobal(name)
}

class LocalVarInfo(name: String, val indexInScope: Int) : VarInfo(name, VarLocation.LOCAL) {
    override val loadOpCode = GetLocal(indexInScope)
    override val storeOpCode = SetLocal(indexInScope)
}

class ClosureVarInfo(localVar: LocalVarInfo, closureDepth: Int) : VarInfo(localVar.name, VarLocation.CLOSURE) {
    override val loadOpCode = GetClosureVar(closureDepth, localVar.indexInScope)
    override val storeOpCode = SetClosureVar(closureDepth, localVar.indexInScope)
}

class ImplicitCtxVarInfo(ctxVar: VarInfo, varName: String) : VarInfo(varName, VarLocation.CTX_OR_GLOBAL) {
    override val loadOpCode = GetCtxOrGlobal(ctxVar.loadOpCode as FastOpCode, varName)
    override val storeOpCode = SetCtxOrGlobal(ctxVar.storeOpCode as FastOpCode, varName)
}

abstract class Scope {
    abstract val parent: Scope?

    open fun getOwnVar(name: String): VarInfo? = null
}

abstract class TopLevelScope : Scope() {
    protected var localVarsIndex = 0

    abstract fun allocate(name: String): VarInfo

    val varsAllocated: Int get() = localVarsIndex
}

abstract class InnerScope(override val parent: Scope) : Scope()

class FunctionScope(override val parent: Scope?, val implicitCtxLookup: Boolean) : TopLevelScope() {
    var closureDepthNeeded = 0

    override fun allocate(name: String): LocalVarInfo {
        return LocalVarInfo(name, localVarsIndex++)
    }
}

class BlockScope(parent: Scope, val varsHere: HashMap<String, VarInfo>) : InnerScope(parent) {
    override fun getOwnVar(name: String) = varsHere[name]
}

fun Scope.topScope(): TopLevelScope {
    var curr = this
    while (true) {
        if (curr is TopLevelScope) {
            return curr
        } else {
            curr = curr.parent ?: throw UnsupportedOperationException("Global scope has no parent")
        }
    }
}