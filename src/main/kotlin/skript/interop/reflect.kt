package skript.interop

import skript.exec.ParamType
import skript.io.SkriptEngine
import kotlin.reflect.*
import kotlin.reflect.full.*

fun <T: Any> reflectNativeClass(klass: KClass<T>, classDef: SkNativeClassDef<T>, engine: SkriptEngine): Boolean {
    val membersByName = HashMap<String, ArrayList<KCallable<*>>>()

    for (member in klass.members) {
        if (member.visibility != KVisibility.PUBLIC)
            continue

        if (member is KProperty2<*, *, *>)
            continue

        val thisParam = member.instanceParameter ?: member.extensionReceiverParameter ?: continue
        val thisClass = thisParam.type.classifier as? KClass<*> ?: continue

        if (!thisClass.isSuperclassOf(klass))
            continue

        // skip standard equals, hashCode and toString
        if (thisClass == Any::class)
            continue

        if (member is KFunction<*>) {
            if (member.name == "toString" && member.valueParameters.isEmpty())
                continue
            if (member.name == "hashCode" && member.valueParameters.isEmpty())
                continue
            if (member.name == "equals" && member.valueParameters.size == 1 && member.valueParameters[0].type == Any::class)
                continue
        }

        membersByName.getOrPut(member.name) { ArrayList() }.add(member)
    }

    for ((_, memberList) in membersByName) {
        val member = memberList.singleOrNull() ?: continue

        when (member) {
            is KFunction<*> ->
                reflectFunction(member, classDef, engine)

            is KMutableProperty1<*,*> ->
                reflectMutableProperty(member as KMutableProperty1<T, *>, classDef, engine)

            is KProperty1<*,*> ->
                reflectReadOnlyProperty(member as KProperty1<T, *>, classDef, engine)

            else ->
                throw IllegalStateException("Unknown type of member encounted in ${classDef.nativeClass} - $member")
        }
    }

    return true
}

fun <T: Any, R> reflectFunction(prop: KFunction<R>, classDef: SkNativeClassDef<T>, engine: SkriptEngine) {
    val returnCodec = engine.getNativeCodec(prop.returnType) ?: return

    val params = prop.valueParameters.map { param ->
        if (param.isVararg) {
            return // TODO()
        }

        val paramName = param.name ?: return
        val paramCodec = engine.getNativeCodec(param.type) ?: return

        ParamInfo(paramName, param, ParamType.NORMAL, paramCodec)
    }

    val thisParam = prop.instanceParameter ?: prop.extensionReceiverParameter ?: return

    val skMethod = SkNativeMethod(prop.name, thisParam, params, returnCodec, prop, classDef)
    classDef.defineInstanceMethod(skMethod)
}

fun <T: Any, R> reflectMutableProperty(prop: KMutableProperty1<T, R>, classDef: SkNativeClassDef<T>, engine: SkriptEngine) {
    val codec = engine.getNativeCodec(prop.returnType) ?: return
    val skProp = SkNativeMutableProperty(prop, codec as SkCodec<R>)
    classDef.defineNativeProperty(skProp)
}

fun <T: Any, R> reflectReadOnlyProperty(prop: KProperty1<T, R>, classDef: SkNativeClassDef<T>, engine: SkriptEngine) {
    val codec = engine.getNativeCodec(prop.returnType) ?: return
    val skProp = SkNativeReadOnlyProperty(prop, codec as SkCodec<R>)
    classDef.defineNativeProperty(skProp)
}