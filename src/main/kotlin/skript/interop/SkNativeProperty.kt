package skript.interop

import skript.io.SkriptEnv
import skript.typeError
import skript.values.*
import kotlin.reflect.KClass

sealed class SkNativeProperty<RCVR: Any, T> : SkObjectProperty() {
    abstract val getter: (RCVR)->T
    abstract val codec: SkCodec<T>
    abstract val nativeClass: KClass<RCVR>

    protected fun getNativeObj(obj: SkObject): RCVR {
        obj as? HoldsNative<*> ?: typeError("Accessing property $name on wrong class object")
        val nativeObj = obj.nativeObj
        @Suppress("UNCHECKED_CAST")
        if (nativeClass.isInstance(nativeObj)) {
            return nativeObj as RCVR
        } else {
            typeError("Accessing property $name on wrong class object")
        }
    }

    override suspend fun getValue(obj: SkObject, env: SkriptEnv): SkValue {
        val nativeObj = getNativeObj(obj)
        return when (val nativeVal = getter.invoke(nativeObj)) {
            null -> SkNull
            else -> codec.toSkript(nativeVal, env)
        }
    }
}

class SkNativeMutableProperty<RCVR: Any, T>(
    override val name: String,
    override val nullable: Boolean,
    override val expectedClass: SkClassDef,
    override val nativeClass: KClass<RCVR>,
    override val codec: SkCodec<T>,
    override val getter: (RCVR) -> T,
    val setter: (RCVR,T)->Unit
) : SkNativeProperty<RCVR, T>() {
    override val readOnly: Boolean get() = false

    override suspend fun setValue(obj: SkObject, value: SkValue, env: SkriptEnv) {
        val nativeObj = getNativeObj(obj)
        val nativeValue = codec.toKotlin(value, env)
        setter.invoke(nativeObj, nativeValue)
    }
}

class SkNativeReadOnlyProperty<RCVR: Any, T>(
    override val name: String,
    override val nullable: Boolean,
    override val expectedClass: SkClassDef,
    override val nativeClass: KClass<RCVR>,
    override val codec: SkCodec<T>,
    override val getter: (RCVR) -> T
) : SkNativeProperty<RCVR, T>() {
    override val readOnly: Boolean
        get() = true

    override suspend fun setValue(obj: SkObject, value: SkValue, env: SkriptEnv) {
        typeError("Can't set read-only property $name")
    }
}