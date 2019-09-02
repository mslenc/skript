package skript.ast

import skript.exec.FunctionDef
import skript.util.AstProps

class Module(val name: String, val content: List<Statement>) {
    lateinit var moduleInit: FunctionDef
    val props = AstProps()
}