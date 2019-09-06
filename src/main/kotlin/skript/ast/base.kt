package skript.ast

import skript.analysis.ModuleScope
import skript.exec.FunctionDef

class Module(val name: String, val content: List<Statement>) {
    lateinit var moduleScope: ModuleScope
    lateinit var moduleInit: FunctionDef
}