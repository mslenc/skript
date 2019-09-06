package skript.analysis

import skript.ast.*
import skript.exec.FunctionDef
import skript.exec.ParamDef
import skript.exec.ParamType
import skript.opcodes.*
import skript.opcodes.compare.*
import skript.opcodes.equals.BinaryEqualsOp
import skript.opcodes.equals.BinaryNotEqualsOp
import skript.opcodes.equals.BinaryStrictEqualsOp
import skript.opcodes.equals.BinaryStrictNotEqualsOp
import skript.opcodes.numeric.*
import skript.syntaxError
import skript.util.Stack
import skript.values.SkNumber
import skript.values.SkUndefined
import skript.withTop

class FunctionDefBuilder(val name: String, val paramDefs: Array<ParamDef>, val localsSize: Int, val framesCaptured: Int) {
    val ops = ArrayList<OpCode>()

    operator fun plusAssign(op: OpCode) {
        ops.add(op)
    }

    operator fun plusAssign(target: JumpTarget) {
        target.value = ops.size
    }

    fun build() = FunctionDef(name, paramDefs, ops.toTypedArray(), localsSize, framesCaptured)
}

data class LoopInfo(
    val label: String?,
    private val continueTarget: JumpTarget?,
    private val breakTarget: JumpTarget?
) {
    var breakUsed = false

    fun getBreakTarget(): JumpTarget? {
        breakUsed = true
        return breakTarget
    }

    fun getContinueTarget(): JumpTarget? {
        return continueTarget
    }
}

class OpCodeGen : StatementVisitor, ExprVisitor {
    val builders = Stack<FunctionDefBuilder>()
    val loops = Stack<LoopInfo>()
    val builder: FunctionDefBuilder
        get() = builders.top()

    fun visitModule(module: Module) {
        val moduleInit = FunctionDefBuilder("moduleInit_${module.name}", emptyArray(), module.moduleScope.varsAllocated, 0)
        builders.withTop(moduleInit) {
            Statements(module.content).accept(this)
        }

        module.moduleInit = moduleInit.build()
    }

    override fun visitBlock(stmts: Statements) {
        for (stmt in stmts.parts) {
            if (stmt is DeclareFunction) {
                stmt.accept(this)
            }
        }

        for (stmt in stmts.parts) {
            if (stmt !is DeclareFunction) {
                stmt.accept(this)
            }
        }
    }

    override fun visitIf(stmt: IfStatement) {
        // if else:
        // condition jmpIfFalse(falseBlock) trueBlock jmp(end) falseBlock <end>

        // if:
        // condition jmpIfFalse(end) trueBlock <end>

        val end = JumpTarget()
        stmt.condition.accept(this)

        if (stmt.ifFalse != null) {
            val falseBlock = JumpTarget()
            builder += JumpIfFalsy(falseBlock)
            stmt.ifTrue.accept(this)
            builder += Jump(end)
            builder += falseBlock
            stmt.ifFalse.accept(this)
            builder += end
        } else {
            builder += JumpIfFalsy(end)
            stmt.ifTrue.accept(this)
            builder += end
        }
    }

    override fun visitExprStmt(stmt: ExpressionStatement) {
        stmt.expression.accept(this)
        builder += Pop
    }

    override fun visitWhile(stmt: WhileStatement) {
        // jmp(check) <start> body <check> condition jmpIfTrue(start) <end>
        val check = JumpTarget()
        val start = JumpTarget()
        val end = JumpTarget()

        // TODO: loop label
        loops.withTop(LoopInfo(null, continueTarget = check, breakTarget = end)) {
            builder += Jump(check)
            builder += start
            stmt.body.accept(this)
            builder += check
            stmt.condition.accept(this)
            builder += JumpIfTruthy(start)
            builder += end
        }
    }

    override fun visitLet(stmt: LetStatement) {
        for (decl in stmt.decls) {
            if (decl.initializer != null) {
                decl.initializer.accept(this)
            } else {
                builder += PushLiteral(SkUndefined)
            }

            when (val varInfo = decl.varInfo) {
                is LocalVarInfo -> {
                    builder += SetLocal(varInfo.indexInScope)
                }
                is ModuleVarInfo -> {
                    builder += SetModuleVar(varInfo.moduleName, varInfo.indexInModule)
                }
                else -> throw IllegalStateException("let statement defining a global?!")
            }
        }
    }

    override fun visitDeclareFunctionStmt(stmt: DeclareFunction) {
        val funcDef = makeFuncDef(stmt)

        builder += MakeFunction(funcDef)
        builder += stmt.hoistedVarInfo.storeOpCode
    }

    fun makeFuncDef(stmt: DeclareFunction): FunctionDef {
        val paramDefs = stmt.params.map {
            ParamDef(it.paramName, it.varInfo.indexInScope, it.paramType)
        }.toTypedArray()

        val funcScope = stmt.innerFunScope

        val funcName = stmt.funcName ?: "<anonFun>" // TODO: improve this name
        val funcBuilder = FunctionDefBuilder(funcName, paramDefs, funcScope.varsAllocated, funcScope.closureDepthNeeded)
        builders.withTop(funcBuilder) {
            if (paramDefs.isNotEmpty()) {
                builder += MakeArgsConstructor(funcName)
                for (param in paramDefs) {
                    builder += when (param.type) {
                        ParamType.NORMAL -> ArgsExtractNamed(param.name, param.localIndex)
                        ParamType.POS_ARGS -> ArgsExtractRemainingPos(param.localIndex)
                        ParamType.KW_ARGS -> ArgsExtractRemainingKw(param.localIndex)
                    }
                }
                builder += Pop
            }

            for (param in stmt.params) {
                if (param.defaultValue != null) {
                    val skip = JumpTarget()
                    funcBuilder += JumpIfLocalDefined(param.varInfo.indexInScope, skip)
                    param.defaultValue.accept(this)
                    funcBuilder += SetLocal(param.varInfo.indexInScope)
                    funcBuilder += skip
                }
            }

            stmt.body.accept(this)
        }

        return funcBuilder.build()
    }

    override fun visitReturnStatement(stmt: ReturnStatement) {
        if (stmt.value != null) {
            stmt.value.accept(this)
        } else {
            builder += PushLiteral(SkUndefined)
        }
        builder += Return
    }

    override fun visitDoWhile(stmt: DoWhileStatement) {
        // <start> body <check> condition jumpIfTrue(start) <end>

        val start = JumpTarget()
        val check = JumpTarget()
        val end = JumpTarget()

        loops.withTop(LoopInfo(null, continueTarget = check, breakTarget = end)) {
            builder += start
            stmt.body.accept(this)
            builder += check
            stmt.condition.accept(this)
            builder += JumpIfTruthy(start)
            builder += end
        }
    }

    override fun visitForStatement(stmt: ForStatement) {
        // container makeIterator <loopStart> iteratorNext(end) store(value) [store(index)] body jump(loopStart) [ <breakTarget> pop ] <end>

        val loopStart = JumpTarget()
        val end = JumpTarget()
        val breakTarget = JumpTarget()

        val doKey = stmt.decls.size >= 2
        val doValue = stmt.decls.size >= 1

        val keyDeclIndex = if (doKey) 0 else -1
        val valDeclIndex = if (doKey) 1 else 0

        val loopInfo = LoopInfo(null, continueTarget = loopStart, breakTarget = breakTarget)
        loops.withTop(loopInfo) {
            stmt.container.accept(this)
            builder += MakeIterator
            builder += loopStart
            builder += IteratorNext(doKey, doValue, end)
            if (doValue) builder += stmt.decls[valDeclIndex].varInfo.storeOpCode
            if (doKey) builder += stmt.decls[keyDeclIndex].varInfo.storeOpCode
            stmt.body.accept(this)
            builder += Jump(loopStart)

            if (loopInfo.breakUsed) {
                builder += breakTarget
                builder += Pop
            }

            builder += end
        }
    }

    private fun findLoopInfo(label: String?): LoopInfo? {
        for (i in 0 until loops.size) {
            val loopInfo = loops.top(i)
            if (label == null || label == loopInfo.label)
                return loopInfo
        }
        return null
    }

    override fun visitBreakStatement(stmt: BreakStatement) {
        val loopInfo = findLoopInfo(stmt.label) ?: when {
            stmt.label != null -> syntaxError("Couldn't find label ${stmt.label}", stmt.pos)
            else -> syntaxError("Nothing to break from here", stmt.pos)
        }

        val target = loopInfo.getBreakTarget() ?: syntaxError("Can't break here", stmt.pos)

        builder += Jump(target)
    }

    override fun visitContinueStatement(stmt: ContinueStatement) {
        val loopInfo = findLoopInfo(stmt.label) ?: when {
            stmt.label != null -> syntaxError("Couldn't find label ${stmt.label}", stmt.pos)
            else -> syntaxError("Nothing to continue in here", stmt.pos)
        }

        val target = loopInfo.getContinueTarget() ?: syntaxError("Can't continue here", stmt.pos)

        builder += Jump(target)
    }

    override fun visitUnaryExpr(expr: UnaryExpression) {
        expr.inner.accept(this)

        builder += when (expr.op) {
            UnaryOp.MINUS -> UnaryMinus
            UnaryOp.PLUS -> UnaryPlus
            UnaryOp.NOT -> UnaryNegate
        }
    }

    private fun loadStoreGenerateLoad(expr: LValue) {
        return when (expr) {
            is Variable -> {
                builder += expr.varInfo.loadOpCode
            }
            is FieldAccess -> {
                expr.obj.accept(this)
                builder += Dup
                builder += GetKnownMember(expr.fieldName)
            }
            is ArrayAccess -> {
                expr.arr.accept(this)
                expr.index.accept(this)
                builder += Dup2
                builder += GetMember
            }
        }
    }

    private fun loadStoreGenerateCopy(expr: LValue) {
        return when (expr) {
            is Variable -> {
                // [ VAL ] -> [ VAL VAL ]
                builder += Dup
            }
            is FieldAccess -> {
                // [ OBJ VAL ] -> [ VAL OBJ VAL ]
                builder += CopyTopTwoDown
            }
            is ArrayAccess -> {
                // [ ARR IDX VAL ] -> [ VAL ARR IDX VAL ]
                builder += CopyTopThreeDown
            }
        }
    }

    private fun loadStoreGenerateStore(expr: LValue, keepValue: Boolean) {
        return when (expr) {
            is Variable -> {
                if (keepValue) {
                    builder += Dup
                }
                builder += expr.varInfo.storeOpCode
            }
            is FieldAccess -> {
                if (keepValue) {
                    builder += SetKnownMemberKeepValue(expr.fieldName)
                } else {
                    builder += SetKnownMember(expr.fieldName)
                }
            }
            is ArrayAccess -> {
                if (keepValue) {
                    builder += SetMemberKeepValue
                } else {
                    builder += SetMember
                }
            }
        }
    }

    override fun visitPrePostExpr(expr: PrePostExpr) {
        return when (expr.op) {
            PrePostOp.PRE_INCR -> {
                loadStoreGenerateLoad(expr.inner)
                builder += PushLiteral(SkNumber.ONE)
                builder += BinaryAddOp
                loadStoreGenerateStore(expr.inner, true)
            }
            PrePostOp.PRE_DECR -> {
                loadStoreGenerateLoad(expr.inner)
                builder += PushLiteral(SkNumber.MINUS_ONE)
                builder += BinaryAddOp
                loadStoreGenerateStore(expr.inner, true)
            }
            PrePostOp.POST_INCR -> {
                loadStoreGenerateLoad(expr.inner)
                loadStoreGenerateCopy(expr.inner)
                builder += PushLiteral(SkNumber.ONE)
                builder += BinaryAddOp
                loadStoreGenerateStore(expr.inner, false)
            }
            PrePostOp.POST_DECR -> {
                loadStoreGenerateLoad(expr.inner)
                loadStoreGenerateCopy(expr.inner)
                builder += PushLiteral(SkNumber.MINUS_ONE)
                builder += BinaryAddOp
                loadStoreGenerateStore(expr.inner, false)
            }
        }
    }

    override fun visitBinaryExpr(expr: BinaryExpression) {
        expr.left.accept(this)
        generateRestOfBinaryOp(expr.op, expr.right)
    }

    override fun visitAssignExpression(expr: AssignExpression) {
        expr.op?.let { binaryOp ->
            loadStoreGenerateLoad(expr.left)
            generateRestOfBinaryOp(binaryOp, expr.right)
            loadStoreGenerateStore(expr.left, true)
            return
        }

        when (expr.left) {
            is Variable -> {
                // nothing yet
            }
            is FieldAccess -> {
                expr.left.obj.accept(this)
            }
            is ArrayAccess -> {
                expr.left.arr.accept(this)
                expr.left.index.accept(this)
            }
        }

        expr.right.accept(this)

        when (expr.left) {
            is Variable -> {
                builder += Dup
                builder += expr.left.varInfo.storeOpCode
            }
            is FieldAccess -> {
                builder += SetKnownMemberKeepValue(expr.left.fieldName)
            }
            is ArrayAccess -> {
                builder += SetMemberKeepValue
            }
        }
    }

    private fun generateRestOfBinaryOp(op: BinaryOp, right: Expression) {
        val simpleOp = when (op) {
            BinaryOp.SUBTRACT -> BinarySubtractOp
            BinaryOp.ADD -> BinaryAddOp
            BinaryOp.MULTIPLY -> BinaryMultiplyOp
            BinaryOp.DIVIDE -> BinaryDivideOp
            BinaryOp.DIVIDE_INT -> BinaryDivideIntOp
            BinaryOp.REMAINDER -> BinaryRemainderOp

            BinaryOp.EQUALS -> BinaryEqualsOp
            BinaryOp.NOT_EQUALS -> BinaryNotEqualsOp

            BinaryOp.STRICT_EQUALS -> BinaryStrictEqualsOp
            BinaryOp.NOT_STRICT_EQUALS -> BinaryStrictNotEqualsOp

            BinaryOp.STARSHIP -> BinaryStarshipOp
            BinaryOp.LESS_THAN -> BinaryLessThanOp
            BinaryOp.LESS_OR_EQUAL -> BinaryLessOrEqualOp
            BinaryOp.GREATER_THAN -> BinaryGreaterThanOp
            BinaryOp.GREATER_OR_EQUAL -> BinaryGreaterOrEqualOp

            BinaryOp.RANGE_TO -> MakeRangeEndInclusive
            BinaryOp.RANGE_TO_EXCL -> MakeRangeEndExclusive

            BinaryOp.OR -> {
                val end = JumpTarget()
                builder += JumpIfTopTruthyElseDrop(end)
                right.accept(this)
                builder += end
                return
            }

            BinaryOp.OR_OR -> {
                val end = JumpTarget()
                builder += JumpIfTopTruthyElseDropAlsoMakeBool(end)
                right.accept(this)
                builder += ConvertToBool
                builder += end
                return
            }

            BinaryOp.AND -> {
                val end = JumpTarget()
                builder += JumpIfTopFalsyElseDrop(end)
                right.accept(this)
                builder += end
                return
            }

            BinaryOp.AND_AND -> {
                val end = JumpTarget()
                builder += JumpIfTopFalsyElseDropAlsoMakeBool(end)
                right.accept(this)
                builder += ConvertToBool
                builder += end
                return
            }

            BinaryOp.ELVIS -> {
                val end = JumpTarget()
                builder += JumpIfTopDefinedElseDrop(end)
                right.accept(this)
                builder += end
                return
            }
        }

        right.accept(this)
        builder += simpleOp
    }

    override fun visitCompareSequence(expr: CompareSequence) {
        for (operand in expr.operands) {
            operand.accept(this)
        }
        builder += CompareSeqOp(expr.ops.toTypedArray())
    }

    override fun visitCompareAllPairs(expr: CompareAllPairs) {
        for (operand in expr.operands) {
            operand.accept(this)
        }
        builder += CompareSeqOp(expr.ops.toTypedArray())
    }

    override fun visitTernaryExpression(expr: TernaryExpression) {
        // cond jumpIfFalse(f) ifTrue jump(end) <f> ifFalse <end>
        val f = JumpTarget()
        val end = JumpTarget()

        expr.cond.accept(this)
        builder += JumpIfFalsy(f)
        expr.ifTrue.accept(this)
        builder += Jump(end)
        builder += f
        expr.ifFalse.accept(this)
        builder += end
    }

    override fun visitVarRef(expr: Variable) {
        builder += expr.varInfo.loadOpCode
    }

    override fun visitFieldAccess(expr: FieldAccess) {
        expr.obj.accept(this)
        builder += GetKnownMember(expr.fieldName)
    }

    override fun visitArrayAccess(expr: ArrayAccess) {
        expr.arr.accept(this)
        expr.index.accept(this)
        builder += GetMember
    }

    override fun visitMethodCall(expr: MethodCall) {
        if (expr.type == MethodCallType.SAFE) {
            val skipTarget = JumpTarget()

            expr.obj.accept(this)
            builder += JumpForSafeMethodCall(skipTarget)
            doArgs(expr.args)
            builder += CallMethod(expr.methodName)
            builder += skipTarget
        } else {
            expr.obj.accept(this)
            doArgs(expr.args)
            builder += CallMethod(expr.methodName)
        }
    }

    override fun visitFunctionCall(expr: FuncCall) {
        expr.func.accept(this)
        doArgs(expr.args)
        builder += CallFunction
    }

    private fun doArgs(args: List<CallArg>) {
        builder += BeginArgs

        for (arg in args) {
            arg.value.accept(this)

            builder += when (arg) {
                is PosArg -> AddPosArg
                is SpreadPosArg -> SpreadPosArgs
                is KwArg -> AddKwArg(arg.name)
                is SpreadKwArg -> SpreadKwArgs
            }
        }
    }

    override fun visitLiteral(expr: Literal) {
        builder += PushLiteral(expr.value)
    }

    override fun visitMapLiteral(expr: MapLiteral) {
        builder += MapNew
        for (part in expr.parts) {
            when (part) {
                is MapLiteralPartFixedKey -> {
                    part.value.accept(this)
                    builder += MapDupSetKnownKey(part.key)
                }
                is MapLiteralPartExprKey -> {
                    part.key.accept(this)
                    part.value.accept(this)
                    builder += MapDupSetKey
                }
                is MapLiteralPartSpread -> {
                    part.value.accept(this)
                    builder += MapDupSpreadValues
                }
            }
        }
    }

    override fun visitListLiteral(expr: ListLiteral) {
        builder += ListNew
        for (part in expr.parts) {
            when {
                part.isSpread -> {
                    part.value.accept(this)
                    builder += ListDupAppendSpread
                }
                part.value is Literal -> {
                    builder += ListDupAppendLiteral(part.value.value)
                }
                else -> {
                    part.value.accept(this)
                    builder += ListDupAppend
                }
            }
        }
    }

    override fun visitObjectIs(expr: ObjectIs) {
        expr.obj.accept(this)
        expr.klass.accept(this)
        builder += if (expr.positive) ObjectIsOp else ObjectIsntOp
    }

    override fun visitValueIn(expr: ValueIn) {
        expr.value.accept(this)
        expr.container.accept(this)
        builder += if (expr.positive) ValueInOp else ValueNotInOp
    }

    override fun visitRange(expr: Range) {
        expr.start.accept(this)
        expr.end.accept(this)
        builder += if (expr.endInclusive) MakeRangeEndInclusive else MakeRangeEndExclusive
    }

    override fun visitFunctionLiteral(expr: FunctionLiteral) {
        val funcDef = makeFuncDef(expr.funDecl)
        builder += MakeFunction(funcDef)
    }
}