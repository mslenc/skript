package skript.analysis

import skript.ast.*
import skript.exec.FunctionDef
import skript.exec.ParamDef
import skript.opcodes.*
import skript.opcodes.compare.*
import skript.opcodes.equals.*
import skript.opcodes.numeric.*
import skript.util.Stack
import skript.values.SkNumber
import skript.values.SkUndefined
import skript.withTop
import java.lang.IllegalStateException

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

class OpCodeGen : StatementVisitor {
    val builders = Stack<FunctionDefBuilder>()

    fun visitModule(module: Module) {
        val moduleInit = FunctionDefBuilder("moduleInit_${module.name}", emptyArray(), module.props[Scope]!!.topScope().varsAllocated, 0)
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

        val builder = builders.top()

        val end = JumpTarget()
        stmt.condition.accept(ExprOpCodeBuilder(builder))

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
        val builder = builders.top()

        stmt.expression.accept(ExprOpCodeBuilder(builder))
        builder += Pop
    }

    override fun visitWhile(stmt: WhileStatement) {
        // jmp(check) <start> body <check> condition jmpIfTrue(start)
        val builder = builders.top()

        val check = JumpTarget()
        val start = JumpTarget()

        builder += Jump(check)
        builder += start
        stmt.inner.accept(this)
        builder += check
        stmt.condition.accept(ExprOpCodeBuilder(builder))
        builder += JumpIfTruthy(start)
    }

    override fun visitLet(stmt: LetStatement) {
        val builder = builders.top()

        for (decl in stmt.decls) {
            if (decl.initializer != null) {
                decl.initializer.accept(ExprOpCodeBuilder(builder))
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
        val paramDefs = stmt.params.map {
            ParamDef(it.paramName, it.varInfo.indexInScope, it.paramType)
        }.toTypedArray()

        val funcScope = stmt.props[Scope] as FunctionScope

        val funcBuilder = FunctionDefBuilder(stmt.funcName, paramDefs, funcScope.varsAllocated, funcScope.closureDepthNeeded)
        builders.withTop(funcBuilder) {
            for (param in stmt.params) {
                if (param.defaultValue != null) {
                    val skip = JumpTarget()
                    funcBuilder += JumpIfLocalDefined(param.varInfo.indexInScope, skip)
                    param.defaultValue.accept(ExprOpCodeBuilder(funcBuilder))
                    funcBuilder += SetLocal(param.varInfo.indexInScope)
                    funcBuilder += skip
                }
            }

            stmt.body.accept(this)
        }

        val funcDef = funcBuilder.build()

        val builder = builders.top()
        builder += MakeFunction(funcDef)
        builder += stmt.props[VarInfo]!!.storeOpCode
    }

    override fun visitReturnStatement(stmt: ReturnStatement) {
        builders.top() += Return
    }
}

class ExprOpCodeBuilder(val builder: FunctionDefBuilder) : ExprVisitor {
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
            BinaryOp.MINUS -> BinarySubtractOp
            BinaryOp.PLUS -> BinaryAddOp
            BinaryOp.TIMES -> BinaryMultiplyOp
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

            BinaryOp.OR_OR -> {
                val end = JumpTarget()
                builder += JumpIfTopTruthyElseDrop(end)
                right.accept(this)
                builder += end
                return
            }

            BinaryOp.AND_AND -> {
                val end = JumpTarget()
                builder += JumpIfTopFalsyElseDrop(end)
                right.accept(this)
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
        expr.obj.accept(this)
        doArgs(expr.args)
        builder += CallMethod(expr.methodName)
    }

    override fun visitFunctionCall(expr: FuncCall) {
        expr.func.accept(this)
        doArgs(expr.args)
        builder += CallFunction
    }

    private fun doArgs(args: List<FuncParam>) {
        builder += BeginArgs

        for (arg in args) {
            arg.value.accept(this)

            builder += when (arg) {
                is PosParam -> AddPosArg
                is SpreadPosParam -> SpreadPosArgs
                is KwParam -> AddKwArg(arg.name)
                is SpreadKwParam -> SpreadKwArgs
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
}