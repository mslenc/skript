package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.typeError
import skript.util.*

open class SkClassDef(val name: String, val superClass: SkClassDef? = null) {
    internal val instanceMethods = HashMap<String, SkMethod>()
    internal val instanceProps = HashMap<String, SkObjectProperty>()
    internal val staticFunctions = HashMap<String, SkFunction>()

    open suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        typeError("Can't construct new instances of $name")
    }

    open fun checkNameAvailable(name: String) {
        check(!instanceMethods.containsKey(name)) { "This class already has a method named $name" }
        check(!instanceProps.containsKey(name)) { "This class already has a property named $name" }
    }

    open fun defineInstanceMethod(method: SkMethod) {
        checkNameAvailable(method.name)
//        check(method.expectedClass.isSameOrSuperClassOf(this)) { "The method doesn't accept this class" }

        instanceMethods[method.name] = method
    }

    open fun defineInstanceProperty(property: SkObjectProperty) {
        checkNameAvailable(property.name)
//        check(property.expectedClass.isSameOrSuperClassOf(this)) { "The property doesn't accept this class" }

        instanceProps[property.name] = property
    }

    fun defineStaticFunction(function: SkFunction, name: String = function.name) {
        check(!staticFunctions.containsKey(name)) { "This class already has a static function named $name" }

        staticFunctions[name] = function
    }

    fun findInstanceMethod(key: String): SkMethod? {
        return instanceMethods[key] ?: superClass?.findInstanceMethod(key)
    }

    fun findInstanceProperty(key: String): SkObjectProperty? {
        return instanceProps[key] ?: superClass?.findInstanceProperty(key)
    }

    fun isInstance(value: SkValue): Boolean {
        if (value !is SkObject)
            return false

        var klass: SkClassDef = value.klass
        while (klass != this)
            klass = klass.superClass ?: return false

        return true
    }
}

abstract class SkObjectProperty {
    abstract val expectedClass: SkClassDef
    abstract val name: String
    abstract val nullable: Boolean
    abstract val readOnly: Boolean

    abstract suspend fun getValue(obj: SkObject, env: SkriptEnv): SkValue
    abstract suspend fun setValue(obj: SkObject, value: SkValue, env: SkriptEnv)
}

abstract class ExtractArg<T>(val name: String) {
    abstract fun extract(args: SkArguments): T
}

class NoCoerce(name: String): ExtractArg<SkValue>(name) {
    override fun extract(args: SkArguments) = args.getParam(name)
}


class ExtractBoolean(name: String, val ifUndefined: Boolean?): ExtractArg<Boolean>(name) {
    override fun extract(args: SkArguments) = args.expectBoolean(name, ifUndefined = ifUndefined)
}

class ExtractBooleanOpt(name: String): ExtractArg<Boolean?>(name) {
    override fun extract(args: SkArguments): Boolean? {
        return when (val param = args.getParam(name)) {
            SkUndefined -> null
            else -> param.asBoolean().value
        }
    }
}


class ExtractString(name: String, val ifUndefined: String?): ExtractArg<String>(name) {
    override fun extract(args: SkArguments) = args.expectString(name, ifUndefined = ifUndefined)
}

class ExtractStringOpt(name: String): ExtractArg<String?>(name) {
    override fun extract(args: SkArguments): String? {
        return when (val param = args.getParam(name)) {
            SkUndefined -> null
            else -> param.asString().value
        }
    }
}


class ExtractNumber(name: String, val ifUndefined: SkNumber?): ExtractArg<SkNumber>(name) {
    override fun extract(args: SkArguments) = args.expectNumber(name, ifUndefined = ifUndefined)
}

class ExtractNumberOpt(name: String): ExtractArg<SkNumber?>(name) {
    override fun extract(args: SkArguments): SkNumber? {
        return when (val param = args.getParam(name)) {
            SkUndefined -> null
            else -> param.asNumber()
        }
    }
}


class ExtractFunction(name: String): ExtractArg<SkFunction>(name) {
    override fun extract(args: SkArguments) = args.expectFunction(name)
}


class ExtractRestPosArgs(name: String) : ExtractArg<List<SkValue>>(name) {
    override fun extract(args: SkArguments) = args.getRemainingPosArgs()
}

class ExtractRestKwArgs(name: String) : ExtractArg<Map<String, SkValue>>(name) {
    override fun extract(args: SkArguments) = args.getRemainingKwArgs()
}


open class SkCustomClass<OBJ>(name: String, superClass: SkClassDef? = null) : SkClassDef(name, superClass) {
    fun defineMethod(methodName: String): MethodBuilder0<OBJ> {
        return MethodBuilder0(this, methodName)
    }

    fun defineReadOnlyProperty(propName: String, getter: suspend (OBJ)->SkValue) {
        defineInstanceProperty(SkCustomReadOnlyProperty(this, propName, getter))
    }

    fun defineMutableProperty(propName: String, getter: suspend (OBJ)->SkValue, setter: suspend (OBJ, SkValue)->Unit) {
        defineInstanceProperty(SkCustomMutableProperty(this, propName, getter, setter))
    }
}

class SkCustomReadOnlyProperty<OBJ>(override val expectedClass: SkClassDef, override val name: String, val getter: suspend (OBJ)->SkValue) : SkObjectProperty() {
    override val nullable: Boolean
        get() = true // TODO

    override val readOnly: Boolean
        get() = true

    override suspend fun getValue(obj: SkObject, env: SkriptEnv): SkValue {
        assert(expectedClass.isInstance(obj))
        return getter(obj as OBJ)
    }

    override suspend fun setValue(obj: SkObject, value: SkValue, env: SkriptEnv) {
        typeError("Property $name is read-only")
    }
}

class SkCustomMutableProperty<OBJ>(override val expectedClass: SkClassDef, override val name: String, val getter: suspend (OBJ)->SkValue, val setter: suspend (OBJ, SkValue)->Unit) : SkObjectProperty() {
    override val nullable: Boolean
        get() = true // TODO

    override val readOnly: Boolean
        get() = false

    override suspend fun getValue(obj: SkObject, env: SkriptEnv): SkValue {
        assert(expectedClass.isInstance(obj))
        return getter(obj as OBJ)
    }

    override suspend fun setValue(obj: SkObject, value: SkValue, env: SkriptEnv) {
        assert(expectedClass.isInstance(obj))
        setter(obj as OBJ, value)
    }
}


class MethodBuilder0<OBJ>(val klass: SkCustomClass<OBJ>, val methodName: String) {
    fun withImpl(impl: suspend (OBJ)->SkValue) {
        klass.defineInstanceMethod(object : SkMethod(methodName, emptyList()) {
            override val expectedClass: SkClassDef
                get() = klass

            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
                args.expectNothingElse()
                return impl(thiz as OBJ)
            }
        })
    }

    fun <T> withParam(param: ExtractArg<T>): MethodBuilder1<OBJ, T> {
        return MethodBuilder1(klass, methodName, param)
    }

    fun withParam(paramName: String) = withParam(NoCoerce(paramName))
    fun withBooleanParam(paramName: String, defaultValue: Boolean? = null) = withParam(ExtractBoolean(paramName, defaultValue))
    fun withStringParam(paramName: String, defaultValue: String? = null) = withParam(ExtractString(paramName, defaultValue))
    fun withNumberParam(paramName: String, defaultValue: SkNumber? = null) = withParam(ExtractNumber(paramName, defaultValue))
    fun withOptBooleanParam(paramName: String) = withParam(ExtractBooleanOpt(paramName))
    fun withOptStringParam(paramName: String) = withParam(ExtractStringOpt(paramName))
    fun withOptNumberParam(paramName: String) = withParam(ExtractNumberOpt(paramName))
    fun withFunctionParam(paramName: String) = withParam(ExtractFunction(paramName))
    fun withRestPosArgs(paramName: String) = withParam(ExtractRestPosArgs(paramName))
    fun withRestKwArgs(paramName: String) = withParam(ExtractRestKwArgs(paramName))
}

class MethodBuilder1<OBJ, T1>(val klass: SkCustomClass<OBJ>, val methodName: String, val param1: ExtractArg<T1>) {
    fun withImpl(impl: suspend (OBJ, T1, RuntimeState)->SkValue) {
        klass.defineInstanceMethod(object : SkMethod(methodName, listOf(param1.name)) {
            override val expectedClass: SkClassDef
                get() = klass

            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
                val arg1 = param1.extract(args)
                args.expectNothingElse()
                return impl(thiz as OBJ, arg1, state)
            }
        })
    }

    fun <T> withParam(param: ExtractArg<T>): MethodBuilder2<OBJ, T1, T> {
        return MethodBuilder2(klass, methodName, param1, param)
    }

    fun withParam(paramName: String) = withParam(NoCoerce(paramName))
    fun withBooleanParam(paramName: String, defaultValue: Boolean? = null) = withParam(ExtractBoolean(paramName, defaultValue))
    fun withStringParam(paramName: String, defaultValue: String? = null) = withParam(ExtractString(paramName, defaultValue))
    fun withNumberParam(paramName: String, defaultValue: SkNumber? = null) = withParam(ExtractNumber(paramName, defaultValue))
    fun withOptBooleanParam(paramName: String) = withParam(ExtractBooleanOpt(paramName))
    fun withOptStringParam(paramName: String) = withParam(ExtractStringOpt(paramName))
    fun withOptNumberParam(paramName: String) = withParam(ExtractNumberOpt(paramName))
    fun withFunctionParam(paramName: String) = withParam(ExtractFunction(paramName))
    fun withRestPosArgs(paramName: String) = withParam(ExtractRestPosArgs(paramName))
    fun withRestKwArgs(paramName: String) = withParam(ExtractRestKwArgs(paramName))
}

class MethodBuilder2<OBJ, T1, T2>(val klass: SkCustomClass<OBJ>, val methodName: String, val param1: ExtractArg<T1>, val param2: ExtractArg<T2>) {
    fun withImpl(impl: suspend (OBJ, T1, T2, RuntimeState)->SkValue) {
        klass.defineInstanceMethod(object : SkMethod(methodName, listOf(param1.name, param2.name)) {
            override val expectedClass: SkClassDef
                get() = klass

            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
                val arg1 = param1.extract(args)
                val arg2 = param2.extract(args)
                args.expectNothingElse()
                return impl(thiz as OBJ, arg1, arg2, state)
            }
        })
    }

    fun <T> withParam(param: ExtractArg<T>): MethodBuilder3<OBJ, T1, T2, T> {
        return MethodBuilder3(klass, methodName, param1, param2, param)
    }

    fun withParam(paramName: String) = withParam(NoCoerce(paramName))
    fun withBooleanParam(paramName: String, defaultValue: Boolean? = null) = withParam(ExtractBoolean(paramName, defaultValue))
    fun withStringParam(paramName: String, defaultValue: String? = null) = withParam(ExtractString(paramName, defaultValue))
    fun withNumberParam(paramName: String, defaultValue: SkNumber? = null) = withParam(ExtractNumber(paramName, defaultValue))
    fun withOptBooleanParam(paramName: String) = withParam(ExtractBooleanOpt(paramName))
    fun withOptStringParam(paramName: String) = withParam(ExtractStringOpt(paramName))
    fun withOptNumberParam(paramName: String) = withParam(ExtractNumberOpt(paramName))
    fun withFunctionParam(paramName: String) = withParam(ExtractFunction(paramName))
    fun withRestPosArgs(paramName: String) = withParam(ExtractRestPosArgs(paramName))
    fun withRestKwArgs(paramName: String) = withParam(ExtractRestKwArgs(paramName))
}

class MethodBuilder3<OBJ, T1, T2, T3>(
    val klass: SkCustomClass<OBJ>, val methodName: String,
    val param1: ExtractArg<T1>,
    val param2: ExtractArg<T2>,
    val param3: ExtractArg<T3>) {

    fun withImpl(impl: suspend (OBJ, T1, T2, T3, RuntimeState)->SkValue) {
        klass.defineInstanceMethod(object : SkMethod(methodName, listOf(param1.name, param2.name, param3.name)) {
            override val expectedClass: SkClassDef
                get() = klass

            override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
                val arg1 = param1.extract(args)
                val arg2 = param2.extract(args)
                val arg3 = param3.extract(args)
                args.expectNothingElse()

                return impl(thiz as OBJ, arg1, arg2, arg3, state)
            }
        })
    }
}