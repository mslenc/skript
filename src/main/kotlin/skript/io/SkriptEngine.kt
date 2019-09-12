package skript.io

import skript.interop.*
import skript.interop.wrappers.SkCodecNativeArray
import skript.interop.wrappers.SkCodecNativeCollection
import skript.interop.wrappers.SkCodecNativeList
import skript.values.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf

class SkriptEngine(val moduleProvider: ParsedModuleProvider = NoModuleProvider, val nativeAccessGranter: NativeAccessGranter = NoNativeAccess) {
    val nativeCodecs = HashMap<KType, SkCodec<*>>()

    init {
        initCodecs(nativeCodecs)
    }

    fun createEnv(initStandardGlobals: Boolean = true): SkriptEnv {
        val env = SkriptEnv(this)

        if (initStandardGlobals) {
            env.apply {
                setGlobal("String", env.getClassObject(SkStringClassDef), true)
                setGlobal("Number", env.getClassObject(SkNumberClassDef), true)
                setGlobal("Boolean", env.getClassObject(SkBooleanClassDef), true)
                setGlobal("Object", env.getClassObject(SkObjectClassDef), true)
                setGlobal("List", env.getClassObject(SkListClassDef), true)
                setGlobal("Map", env.getClassObject(SkMapClassDef), true)
                setGlobal("Regex", env.getClassObject(SkRegexClassDef), true)
            }
        }

        return env
    }

    fun getNativeCodec(type: KType): SkCodec<*>? {
        nativeCodecs[type]?.let { return it }

        if (type.arguments.isEmpty()) {
            val klass = type.classifier as? KClass<*> ?: return null

            return if (klass != Any::class && nativeAccessGranter.isAccessAllowed(klass)) {
                getNativeCodec(klass)
            } else {
                null
            }
        }

        val codec = when {
            type.arguments.size == 1 -> {
                val container = type.classifier as? KClass<*> ?: return null

                when {
                    container.java.isArray -> {
                        // Array<X>
                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        SkCodecNativeArray(innerType)
                    }
                    container.isSubclassOf(List::class) -> {
                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        SkCodecNativeList(innerType)
                    }
                    container.isSubclassOf(Collection::class) -> {
                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        SkCodecNativeCollection(innerType)
                    }
//                    TODO: support gqlktx's Maybe somehow
//                    container.isSubclassOf(Maybe::class) -> {
//                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
//                        return SkCodecMaybe(innerType)
//                    }
                    else -> return null
                }
            }

            // TODO: do at least Map
            else -> return null
        }

        nativeCodecs[type] = codec
        return codec
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getNativeCodec(klass: KClass<T>): SkCodec<T>? {
        // TODO: somehow distinguish nullable/non-nullable?
        // TODO: super classes

        val nonNullType = klass.createType(nullable = false)
        nativeCodecs[nonNullType]?.let { return it as SkCodec<T> }

        val nullableType = klass.createType(nullable = true)

        val className = klass.qualifiedName ?: throw UnsupportedOperationException("Can't use anonymous classes")
        val classDef = SkNativeClassDef(className, klass, null)
        val result = SkCodecNativeObject(classDef)

        // we first store it, then go digging in; that way we only look at each class once
        nativeCodecs[nullableType] = result
        nativeCodecs[nonNullType] = result

        if (!reflectNativeClass(klass, classDef, this)) {
            nativeCodecs.remove(nullableType)
            nativeCodecs.remove(nonNullType)
            return null
        }

        return result
    }
}