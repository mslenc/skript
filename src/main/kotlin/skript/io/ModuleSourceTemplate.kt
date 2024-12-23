package skript.io

import skript.analysis.OpCodeGen
import skript.analysis.VarAllocator
import skript.ast.*
import skript.exec.ParamType
import skript.parser.*
import skript.syntaxError
import java.lang.Character.isJavaIdentifierPart

data class ModuleSourceTemplate(override val moduleName: ModuleName, val source: String, val fileName: String = moduleName.name) : ModuleSource() {
    override fun prepare(engine: SkriptEngine): PreparedModuleSkript {
        val tokens = Tokens(CharStream(source, fileName).lexPageTemplate().cleanUpStmtOnlyLines())
        val parsed = PageTemplateParser(tokens).parsePageTemplate()
        val rewritten = rewriteTemplate(parsed, moduleName, fileName)
        val moduleScope = VarAllocator(moduleName).visitModule(rewritten)
        val moduleInit = OpCodeGen(moduleName).visitModule(moduleScope, rewritten)

        return PreparedModuleSkript(moduleName, moduleScope.varsAllocated, moduleInit)
    }
}

/**
 * So here we rewrite the template, its blocks, extends, and includes, so that they become a module like this:
 *
 * ```
 * import * as parentTemplate from "parent"
 * import * as included_template_1 from "included_template_1"
 *
 * fun renderBlock_foo(ctx, runtime, currBlocks) {
 *     // the block contents
 *
 *     // include becomes
 *     included_template_1.render(ctx, runtime)
 *
 *     // include block becomes
 *     renderBlock_bar(ctx, runtime, currBlocks)
 *
 *     // include super block becomes
 *     parentTemplate.blocks.foo(ctx, runtime, currBlocks)
 *
 *     // declare block just causes the function definition to be emitted, without being called (at that site)
 *     // regular block creates the function definition, and also calls it immediately
 *
 *     // any with { context: vars } uses the literal specified (which may or may not include **ctx), and if
 *     // there isn't one, the default is simply { **ctx }
 *     // (we don't want the child template to be able to modify it)
 * }
 *
 * export val blocks = {
 *     **parentTemplate.blocks,
 *     foo: renderBlock_foo,
 *     bar: renderBlock_bar,
 * }
 *
 * export fun renderWithBlocks(ctx, runtime, currBlocks) {
 *     parentTemplate.renderWithBlocks(ctx, runtime, currBlocks)
 *
 *     // followed by the remainder of the template, with block calls rewriting to:
 *     if (!parentTemplate.blocks.foo) {
 *         renderBlock_foo(ctx, runtime, currBlocks)
 *     }
 * }
 *
 * export fun render(ctx, runtime) {
 *     renderWithBlocks(ctx, runtime, blocks)
 * }
 *
 *
 * ```
 */
fun rewriteTemplate(template: List<Statement>, moduleName: ModuleName, fileName: String): List<Statement> {
    val NO_POS = Pos(1, 1, fileName)

    val finder = ExtendsIncludesAndBlocksFinder()
    for (stmt in template)
        stmt.accept(finder)

    val extends: List<ExtendsStatement> = finder.extends
    val includes: List<IncludeStatement> = finder.includes
    val blockDeclares: List<Pair<String, TemplateBlockStatement>> = finder.blockDeclares
    val blockCalls: List<Pair<String, TemplateBlockStatement>> = finder.blockCalls

    run {
        val seen = HashSet<String>()
        for ((blockName, decl) in blockDeclares)
            if (!seen.add(blockName))
                syntaxError("Block \"${ blockName }\" declared multiple times.", decl.pos)
    }

    val out = ArrayList<Statement>()
    when (extends.size) {
        0 -> {
            // val parentTemplate = { blocks: { } }
            out += LetStatement(
                listOf(VarDecl("parentTemplate", MapLiteral(listOf(MapLiteralPartFixedKey("blocks", MapLiteral(emptyList())))), pos = NO_POS)), export = false
            )
        }
        1 -> {
            // import * as parentTemplate from "..."
            val e = extends.single()
            out += ImportStatement(listOf(ImportDecl(null, "parentTemplate", e.pos)), e.templateName, e.pos)

            e.rewritten = EmptyStatement // done in amendMainBody; should be just require it to be first? probably..
        }
        else -> {
            syntaxError("Multiple extends statements found", extends[1].pos)
        }
    }

    val includeNames = HashMap<String, String>()
    for (inc in includes) {
        val varName: String

        if (!includeNames.containsKey(inc.templateName)) {
            // import * as varName from "..."
            varName = genIncName(inc.templateName, includeNames.keys)
            includeNames[inc.templateName] = varName
            out += ImportStatement(listOf(ImportDecl(null, varName, inc.pos)), inc.templateName, inc.pos)
        } else {
            varName = includeNames.getValue(inc.templateName)
        }

        val ctxArg = inc.ctxParams ?: MapLiteral(listOf(MapLiteralPartSpread(Variable("ctx", inc.pos))))
        val args = listOf(
            PosArg(ctxArg),
            PosArg(Variable("templateRuntime", inc.pos)),
        )

        val renderFunc = ElementAccess(Variable(varName, inc.pos), Literal("render".toSkript()))

        inc.rewritten = ExpressionStatement(FuncCall(renderFunc, args))
    }

    for ((blockName, decl) in blockDeclares) {
        // fun renderBlock_[blockName](ctx, templateRuntime, currBlocks)
        val funcName = renderBlockFuncName(blockName)
        val content = decl.content ?: throw IllegalStateException("Missing content for block declaration.")

        out += DeclareFunction(funcName, renderFuncParams, content.asStatements(), decl.pos, export = false, implicitCtxLookup = true)

        if (!decl.execute) {
            decl.rewritten = EmptyStatement
        }
    }

    for ((blockName, decl) in blockCalls) {
        val ctxArg = decl.ctxParams ?: MapLiteral(listOf(MapLiteralPartSpread(Variable("ctx", decl.pos))))
        val args = listOf(
            PosArg(ctxArg),
            PosArg(Variable("templateRuntime", decl.pos)),
            PosArg(Variable("currBlocks", decl.pos)),
        )

        val parentBlocks = ElementAccess(Variable("parentTemplate", decl.pos), Literal("blocks".toSkript()))
        val parentBlock = ElementAccess(parentBlocks, Literal(blockName.toSkript()))

        val blockFunc = when (decl.fromParent) {
            true -> parentBlock
            else -> ElementAccess(Variable("currBlocks", decl.pos), Literal(blockName.toSkript()))
        }

        val blockExec = ExpressionStatement(FuncCall(blockFunc, args))

        if (decl.declare) {
            decl.rewritten = IfStatement(UnaryExpression(UnaryOp.NOT, parentBlock), blockExec, null)
        } else {
            decl.rewritten = blockExec
        }
    }

    // export val blocks = { **parentTemplate.blocks, ... }
    out += LetStatement(listOf(VarDecl("blocks", MapLiteral(
        listOf(MapLiteralPartSpread(PropertyAccess(Variable("parentTemplate", NO_POS), "blocks"))) +
        blockDeclares.map { (blockName, decl) ->
            MapLiteralPartFixedKey(blockName, Variable(renderBlockFuncName(blockName), decl.pos))
        }
    ), NO_POS)), export = true)

    // export fun renderWithBlocks(ctx, templateRuntime, currBlocks)
    out += DeclareFunction("renderWithBlocks", renderFuncParams, amendMainBody(template, extends.isNotEmpty(), NO_POS), NO_POS, export = true, implicitCtxLookup = true)

    // export fun render(ctx, templateRuntime)
    out += DeclareFunction("render", renderFuncParams.take(2), Statements(listOf(
        ExpressionStatement(FuncCall(Variable("renderWithBlocks", NO_POS), renderFuncArgs(NO_POS, "blocks")))
    )), NO_POS, export = true)

    return out
}

fun amendMainBody(content: List<Statement>, hasParent: Boolean, NO_POS: Pos): Statements {
    val amended = when (hasParent) {
        false -> content
        else -> {
            val out = ArrayList<Statement>()
            out += ExpressionStatement(MethodCall(Variable("parentTemplate", NO_POS), "renderWithBlocks", renderFuncArgs(NO_POS), MethodCallType.REGULAR))
            out.addAll(content)
            out
        }
    }

    return Statements(amended)
}

val renderFuncParams = listOf(
    ParamDecl("ctx", ParamType.NORMAL, null),
    ParamDecl("templateRuntime", ParamType.NORMAL, null),
    ParamDecl("currBlocks", ParamType.NORMAL, null),
)

fun renderFuncArgs(pos: Pos, blocks: String = "currBlocks") = listOf(
    PosArg(Variable("ctx", pos)),
    PosArg(Variable("templateRuntime", pos)),
    PosArg(Variable(blocks, pos)),
)

fun Statement.asStatements(): Statements {
    return when (this) {
        is Statements -> this
        is EmptyStatement -> Statements(emptyList())
        else -> Statements(listOf(this))
    }
}

fun renderBlockFuncName(blockName: String): String {
    return "renderBlock_$blockName"
}

fun genIncName(templateName: String, existing: Set<String>): String {
    val base = "inc_" + templateName.map { if (isJavaIdentifierPart(it)) it else '_' }.joinToString("")
    for (i in 0..100) {
        val modified = if (i == 0) base else base + "_" + i
        if (modified !in existing)
            return modified
    }

    throw IllegalStateException("Couldn't generate variable name for template name $templateName")
}

class ExtendsIncludesAndBlocksFinder : AbstractStatementVisitor() {
    val extends = ArrayList<ExtendsStatement>()
    val includes = ArrayList<IncludeStatement>()
    val blockDeclares = ArrayList<Pair<String, TemplateBlockStatement>>()
    val blockCalls = ArrayList<Pair<String, TemplateBlockStatement>>()
    val blockStack = ArrayList<String>() // names of blocks we're currently in, for "include super block" support

    override fun visitExtendsStatement(stmt: ExtendsStatement) {
        extends += stmt
    }

    override fun visitIncludeStatement(stmt: IncludeStatement) {
        includes += stmt
    }

    override fun visitTemplateBlockStatement(stmt: TemplateBlockStatement) {
        val blockName = stmt.blockName ?: blockStack.lastOrNull() ?: syntaxError("Include super block can't be used outside a block.", stmt.pos)
        stmt.resolvedBlockName = blockName

        if (stmt.declare)
            blockDeclares += Pair(blockName, stmt)

        if (stmt.execute)
            blockCalls += Pair(blockName, stmt)

        if (stmt.content != null) {
            blockStack += blockName
            try {
                stmt.content.accept(this)
            } finally {
                blockStack.removeLast()
            }
        }
    }
}

