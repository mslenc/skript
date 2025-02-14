package skript.io

import skript.interop.*
import skript.interop.wrappers.SkCodecNativeArray
import skript.interop.wrappers.SkCodecNativeCollection
import skript.interop.wrappers.SkCodecNativeList
import skript.interop.wrappers.SkCodecNativeMap
import skript.parser.isValidSkriptIdentifier
import skript.values.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

class SkriptEngine(val nativeAccessGranter: NativeAccessGranter = NoNativeAccess, val userData: Any? = null) {
    val nativeCodecs = HashMap<KType, SkCodec<*>>()
    val nativeClassDefs = HashMap<KClass<*>, SkNativeClassDef<*>>()

    init {
        initCodecs(nativeCodecs)
    }

    fun createEnv(initStandardGlobals: Boolean = true, moduleProvider: ModuleProvider = NoModuleProvider, moduleNameResolver: ModuleNameResolver = NoNameResolver): SkriptEnv {
        val env = SkriptEnv(this, moduleProvider, moduleNameResolver)

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
        val classifier = type.classifier
        if (classifier is KClass<*> && classifier.isSubclassOf(SkValue::class)) {
            return SkCodecSkValue
        }

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
                        // Array<X>, because we pre-register all primitive array types
                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        SkCodecNativeArray(container, container.java.componentType, innerType)
                    }
                    container.isSubclassOf(List::class) -> {
                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        SkCodecNativeList(container, innerType)
                    }
                    container.isSubclassOf(Collection::class) -> {
                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        SkCodecNativeCollection(container, innerType)
                    }


//                    TODO: support gqlktx's Maybe somehow
//                    container.isSubclassOf(Maybe::class) -> {
//                        val innerType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
//                        return SkCodecMaybe(innerType)
//                    }
                    else -> return null
                }
            }

            type.arguments.size == 2 -> {
                val container = type.classifier as? KClass<*> ?: return null

                when {
                    container.isSubclassOf(Map::class) -> {
                        val keyType = getNativeCodec(type.arguments[0].type ?: return null) ?: return null
                        val valueType = getNativeCodec(type.arguments[1].type ?: return null) ?: return null
                        SkCodecNativeMap(container, keyType, valueType)
                    }

                    else -> return null
                }
            }

            else -> return null
        }

        nativeCodecs[type] = codec
        return codec
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getNativeCodec(klass: KClass<T>): SkCodec<T>? {
        // TODO: somehow distinguish nullable/non-nullable?
        // TODO: super classes

        if (klass.findAnnotation<SkriptIgnore>() != null) {
            return null
        }

        val nonNullType = klass.createType(nullable = false)
        nativeCodecs[nonNullType]?.let { return it as SkCodec<T> }

        val nullableType = klass.createType(nullable = true)

        val className = klass.qualifiedName ?: throw UnsupportedOperationException("Can't use anonymous classes")
        val classDef = SkNativeClassDef(className, klass, null)
        val result = SkCodecNativeObject(classDef)

        // we first store it, then go digging in; that way we only look at each class once
        nativeCodecs[nullableType] = result
        nativeCodecs[nonNullType] = result
        nativeClassDefs[klass] = classDef
        reflectNativeClass(klass, classDef, this)

        return result
    }

    fun <T: Any> getNativeClassDef(klass: KClass<T>): SkNativeClassDef<T>? {
        getNativeCodec(klass)
        return nativeClassDefs[klass] as SkNativeClassDef<T>?
    }

    fun <T: Any> requireNativeCodec(klass: KClass<T>): SkCodec<T> {
        return getNativeCodec(klass) ?: throw IllegalStateException("Couldn't reflect $klass")
    }

    companion object {
        fun isValidIdentifier(ident: String): Boolean {
            return isValidSkriptIdentifier(ident)
        }
    }
}