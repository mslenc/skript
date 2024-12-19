package skript.interop

import skript.io.SkriptEngine
import skript.io.SkriptIgnore
import skript.values.SkFunction
import skript.values.SkMethod
import skript.values.SkObjectProperty
import skript.values.SkStaticProperty
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.*
import kotlin.reflect.full.*

fun shouldIgnoreFunction(func: KFunction<*>): Boolean {
    return when (func.name) {
        "toString" -> func.valueParameters.isEmpty()
        "hashCode" -> func.valueParameters.isEmpty()
        "equals" -> func.valueParameters.size == 1 && func.valueParameters[0].type.classifier == Any::class
        else -> false
    }
}

fun shouldIgnore(member: KCallable<*>): Boolean {
    return when {
        member.visibility != KVisibility.PUBLIC -> true
        member.findAnnotation<SkriptIgnore>() != null -> true
        member is KProperty2<*, *, *> -> true
        member is KFunction<*> && shouldIgnoreFunction(member) -> true
        else -> false
    }
}

fun <RCVR, T, RES> ((RCVR,T)->RES).bind(receiver: RCVR): (T)->RES {
    return { param -> invoke(receiver, param) }
}

fun <RCVR, RES> ((RCVR)->RES).bind(receiver: RCVR): ()->RES {
    return { invoke(receiver) }
}


fun <RCVR: Any> reflectCompanionMembers(companionClass: KClass<RCVR>, staticsByName: HashMap<String, ArrayList<SkClassStaticMember>>, engine: SkriptEngine) {
    val companion = companionClass.objectInstance ?: return

    nextMember@
    for (member in companionClass.members) {
        if (shouldIgnore(member))
            continue

        val thisParam = member.instanceParameter ?: member.extensionReceiverParameter
        val reflected: SkClassStaticMember = if (thisParam != null) {
            val thisClass = thisParam.type.classifier as? KClass<*> ?: continue
            if (!thisClass.isSuperclassOf(companionClass))
                continue

            when (member) {
                is KFunction<*> -> {
                    val thisParamInfo = SkNativeParamConst(thisParam, companion)
                    makeNativeStaticFunction(member, engine, thisParamInfo) ?: continue@nextMember
                }

                is KMutableProperty1<*,*> ->
                    reflectBoundMutableProperty(member as KMutableProperty1<RCVR, *>, companion, engine) ?: continue@nextMember

                is KProperty1<*,*> ->
                    reflectBoundReadOnlyProperty(member as KProperty1<RCVR, *>, companion, engine) ?: continue@nextMember

                else ->
                    throw IllegalStateException("Unknown type of companion instance member encountered in $companionClass - $member")
            }
        } else {
            when (member) {
                is KFunction<*> ->
                    makeNativeStaticFunction(member, engine) ?: continue@nextMember

                is KMutableProperty0<*> ->
                    reflectMutableStaticProperty(member, engine) ?: continue@nextMember

                is KProperty0<*> ->
                    reflectReadOnlyStaticProperty(member, engine) ?: continue@nextMember

                else ->
                    throw IllegalStateException("Unknown type of static member encountered in $companionClass - $member")
            }
        }

        staticsByName.getOrPut(reflected.name) { ArrayList() }.add(reflected)
    }
}

fun <T: Any> reflectNativeClass(klass: KClass<T>, classDef: SkNativeClassDef<T>, engine: SkriptEngine) {
    val membersByName = HashMap<String, ArrayList<SkClassInstanceMember>>()
    val staticsByName = HashMap<String, ArrayList<SkClassStaticMember>>()

    if (klass.isSubclassOf(Enum::class)) {
        EnumHelper.getEnumValues(klass.java).forEach { (name, enum) ->
            val prop = SkNativeStaticReadOnlyProperty(
                name = name,
                nullable = false,
                codec = (engine.getNativeCodec(klass)) as SkCodec<T>,
                getter = { enum }
            )

            staticsByName.getOrPut(name) { ArrayList() }.add(prop)
        }
    }

    nextMember@
    for (member in klass.members) {
        if (member.name == "ENUMS" && klass.isSubclassOf(Enum::class) && member is KProperty0)
            continue

        if (shouldIgnore(member))
            continue

        val thisParam = member.instanceParameter ?: member.extensionReceiverParameter
        if (thisParam != null) {
            val thisClass = thisParam.type.classifier as? KClass<*> ?: continue
            if (!thisClass.isSuperclassOf(klass))
                continue

            val reflected: SkClassInstanceMember = when (member) {
                is KFunction<*> ->
                    makeNativeInstanceMethod(member, classDef, engine) ?: continue@nextMember

                is KMutableProperty1<*,*> ->
                    reflectMutableProperty(member as KMutableProperty1<T, *>, classDef, engine) ?: continue@nextMember

                is KProperty1<*,*> ->
                    reflectReadOnlyProperty(member as KProperty1<T, *>, classDef, engine) ?: continue@nextMember

                else ->
                    throw IllegalStateException("Unknown type of instance member encountered in ${classDef.nativeClass} - $member")
            }

            membersByName.getOrPut(reflected.name) { ArrayList() }.add(reflected)
        } else {
            val reflected: SkClassStaticMember = when (member) {
                is KFunction<*> ->
                    makeNativeStaticFunction(member, engine) ?: continue@nextMember

                is KMutableProperty0<*> ->
                    reflectMutableStaticProperty(member, engine) ?: continue@nextMember

                is KProperty0<*> ->
                    reflectReadOnlyStaticProperty(member, engine) ?: continue@nextMember

                else ->
                    throw IllegalStateException("Unknown type of static member encountered in ${classDef.nativeClass} - $member")
            }

            staticsByName.getOrPut(reflected.name) { ArrayList() }.add(reflected)
        }
    }

    klass.companionObject?.let { reflectCompanionMembers(it, staticsByName, engine) }

    nextMember@
    for ((_, memberList) in membersByName) {
        val single: SkClassInstanceMember? = memberList.singleOrNull()
        if (single != null) {
            if (single is SkMethod) {
                classDef.defineInstanceMethod(single)
            } else {
                classDef.defineInstanceProperty(single as SkObjectProperty)
            }
            continue
        }

        if (memberList.any { it is SkObjectProperty }) {
            // we either have multiple properties with the same name (?), or both properties and functions, neither of which we can handle
            continue
        }
    }

    for ((_, staticList) in staticsByName) {
        val single: SkClassStaticMember? = staticList.singleOrNull()
        if (single != null) {
            if (single is SkFunction) {
                classDef.defineStaticFunction(single)
            } else {
                classDef.defineStaticProperty(single as SkStaticProperty)
            }
            continue
        }

        if (staticList.any { it is SkStaticProperty}) {
            // we either have multiple properties with the same name (?), or both properties and functions, neither of which we can handle
            continue
        }
    }

    val constructors = klass.constructors.filter { cons ->
        cons.visibility == KVisibility.PUBLIC &&
        cons.findAnnotation<SkriptIgnore>() == null &&
        cons.parameters.all { engine.getNativeCodec(it.type) != null }
    }

    val constructor: KFunction<T>? = when {
        constructors.isEmpty() -> null
        constructors.size == 1 -> constructors.single()
        else -> {
            val primary = klass.primaryConstructor
            if (primary != null && primary in constructors) {
                primary
            } else {
                null
            }
        }
    }

    constructor?.let { makeNativeConstructor(it, classDef, engine)?.let { cons -> classDef.constructor = cons } }
}

fun makeParamInfos(function: KFunction<*>, engine: SkriptEngine, vararg extraParams: SkNativeParam): SkNativeParams? {
    val result = ArrayList<SkNativeParam>()

    extraParams.forEach { result.add(it) }

    var afterVarArg = false

    function.valueParameters.forEach { param ->
        val paramName = param.name ?: return null
        val paramCodec = engine.getNativeCodec(param.type) ?: return null

        if (param.isVararg) {
            result += SkNativeParamRestArgs(paramName, param, paramCodec)
            afterVarArg = true
        } else {
            if (afterVarArg) {
                result += SkNativeParamKwOnly(paramName, param, paramCodec)
            } else {
                result += SkNativeParamNormal(paramName, param, paramCodec)
            }
        }
    }

    return SkNativeParams(result)
}

fun <T: Any, R> makeNativeInstanceMethod(prop: KFunction<R>, classDef: SkNativeClassDef<T>, engine: SkriptEngine): SkNativeMethod<*>? {
    return SkNativeMethod(
        name = prop.name,
        thisParam = prop.instanceParameter ?: prop.extensionReceiverParameter ?: return null,
        params = makeParamInfos(prop, engine) ?: return null,
        resultCodec = engine.getNativeCodec(prop.returnType) ?: return null,
        impl = prop,
        expectedClass = classDef
    )
}

fun <R> makeNativeStaticFunction(prop: KFunction<R>, engine: SkriptEngine, vararg extraParams: SkNativeParam): SkNativeFunction<*>? {
    return SkNativeFunction(
        name = prop.name,
        params = makeParamInfos(prop, engine, *extraParams) ?: return null,
        impl = prop,
        resultCodec = engine.getNativeCodec(prop.returnType) ?: return null
    )
}

fun <T: Any> makeNativeConstructor(impl: KFunction<T>, classDef: SkNativeClassDef<T>, engine: SkriptEngine): SkNativeConstructor<T>? {
    return SkNativeConstructor(
        name = classDef.className,
        params = makeParamInfos(impl, engine) ?: return null,
        impl = impl,
        skClass = classDef
    )
}


fun <RCVR: Any, T> reflectMutableProperty(prop: KMutableProperty1<RCVR, T>, classDef: SkNativeClassDef<RCVR>, engine: SkriptEngine): SkNativeMutableProperty<RCVR, T>? {
    return SkNativeMutableProperty(
        name = prop.name,
        nullable = prop.returnType.isMarkedNullable,
        expectedClass = classDef,
        nativeClass = classDef.nativeClass,
        codec = (engine.getNativeCodec(prop.returnType) ?: return null) as SkCodec<T>,
        getter = prop.getter,
        setter = prop.setter
    )
}

fun <RCVR: Any, T> reflectBoundMutableProperty(prop: KMutableProperty1<RCVR, T>, instance: RCVR, engine: SkriptEngine): SkNativeStaticMutableProperty<T>? {
    return SkNativeStaticMutableProperty(
        name = prop.name,
        nullable = prop.returnType.isMarkedNullable,
        codec = (engine.getNativeCodec(prop.returnType) ?: return null) as SkCodec<T>,
        getter = prop.getter.bind(instance),
        setter = prop.setter.bind(instance)
    )
}

fun <RCVR: Any, T> reflectReadOnlyProperty(prop: KProperty1<RCVR, T>, classDef: SkNativeClassDef<RCVR>, engine: SkriptEngine): SkNativeReadOnlyProperty<RCVR, T>? {
    return SkNativeReadOnlyProperty(
        name = prop.name,
        nullable = prop.returnType.isMarkedNullable,
        expectedClass = classDef,
        nativeClass = classDef.nativeClass,
        codec = (engine.getNativeCodec(prop.returnType) ?: return null) as SkCodec<T>,
        getter = prop.getter
    )
}

fun <RCVR: Any, T> reflectBoundReadOnlyProperty(prop: KProperty1<RCVR, T>, instance: RCVR, engine: SkriptEngine): SkNativeStaticReadOnlyProperty<T>? {
    return SkNativeStaticReadOnlyProperty(
        name = prop.name,
        nullable = prop.returnType.isMarkedNullable,
        codec = (engine.getNativeCodec(prop.returnType) ?: return null) as SkCodec<T>,
        getter = prop.getter.bind(instance)
    )
}

fun <T> reflectMutableStaticProperty(prop: KMutableProperty0<T>, engine: SkriptEngine): SkNativeStaticMutableProperty<T>? {
    return SkNativeStaticMutableProperty(
        name = prop.name,
        nullable = prop.returnType.isMarkedNullable,
        codec = (engine.getNativeCodec(prop.returnType) ?: return null) as SkCodec<T>,
        getter = prop.getter,
        setter = prop.setter
    )
}

fun <T> reflectReadOnlyStaticProperty(prop: KProperty0<T>, engine: SkriptEngine): SkNativeStaticReadOnlyProperty<T>? {
    return SkNativeStaticReadOnlyProperty(
        name = prop.name,
        nullable = prop.returnType.isMarkedNullable,
        codec = (engine.getNativeCodec(prop.returnType) ?: return null) as SkCodec<T>,
        getter = prop.getter
    )
}