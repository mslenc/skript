package skript.analysis

import skript.ast.Module
import skript.opcodes.*
import java.lang.UnsupportedOperationException

enum class VarLocation {
    GLOBAL,
    MODULE,
    LOCAL,
    CLOSURE
}

sealed class VarInfo(val name: String, val location: VarLocation) {
    abstract val loadOpCode: OpCode
    abstract val storeOpCode: OpCode
}

class GlobalVarInfo(name: String) : VarInfo(name, VarLocation.GLOBAL) {
    override val loadOpCode = GetGlobal(name)
    override val storeOpCode = SetGlobal(name)
}

class ModuleVarInfo(varName: String, val moduleName: String, val indexInModule: Int) : VarInfo(varName, VarLocation.MODULE) {
    override val loadOpCode = GetModuleVar(moduleName, indexInModule)
    override val storeOpCode = SetModuleVar(moduleName, indexInModule)
}

class LocalVarInfo(name: String, val scope: FunctionScope, val indexInScope: Int) : VarInfo(name, VarLocation.LOCAL) {
    override val loadOpCode = GetLocal(indexInScope)
    override val storeOpCode = SetLocal(indexInScope)
}

class ClosureVarInfo(localVar: LocalVarInfo, closureDepth: Int) : VarInfo(localVar.name, VarLocation.CLOSURE) {
    override val loadOpCode = GetClosureVar(closureDepth, localVar.indexInScope)
    override val storeOpCode = SetClosureVar(closureDepth, localVar.indexInScope)
}


abstract class Scope {
    abstract val parent: Scope

    open fun getOwnVar(name: String): VarInfo? = null
}

abstract class TopLevelScope() : Scope() {
    protected var localVarsIndex = 0

    abstract fun allocate(name: String): VarInfo

    val varsAllocated: Int get() = localVarsIndex
}

abstract class InnerScope(override val parent: Scope) : Scope() {

}

class GlobalScope : Scope() {
    override val parent: Scope
        get() = throw UnsupportedOperationException("Global scope has no parent") // TODO: we could also return this, or we could return null (but that makes everything else nullable)
}

class ModuleScope(val module: Module, override val parent: GlobalScope) : TopLevelScope() {
    override fun allocate(name: String): ModuleVarInfo {
        return ModuleVarInfo(name, module.name, localVarsIndex++)
    }
}

class FunctionScope(override val parent: Scope) : TopLevelScope() {
    var closureDepthNeeded = 0

    override fun allocate(name: String): LocalVarInfo {
        return LocalVarInfo(name, this, localVarsIndex++)
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
            curr = curr.parent
        }
    }
}