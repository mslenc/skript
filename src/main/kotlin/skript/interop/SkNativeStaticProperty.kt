package skript.interop

import skript.io.SkriptEnv
import skript.typeError
import skript.values.SkNull
import skript.values.SkStaticProperty
import skript.values.SkValue

sealed class SkNativeStaticProperty<T> : SkStaticProperty() {
    abstract val getter: ()->T
    abstract val codec: SkCodec<T>

    override suspend fun getValue(env: SkriptEnv): SkValue {
        return when (val nativeVal = getter.invoke()) {
            null -> SkNull
            else -> codec.toSkript(nativeVal, env)
        }
    }
}

class SkNativeStaticMutableProperty<T>(
    override val name: String,
    override val nullable: Boolean,
    override val codec: SkCodec<T>,
    override val getter: () -> T,
    val setter: (T)->Unit
) : SkNativeStaticProperty<T>() {
    override val readOnly: Boolean get() = false

    override suspend fun setValue(value: SkValue, env: SkriptEnv) {
        val nativeValue = codec.toKotlin(value, env)
        setter.invoke(nativeValue)
    }
}

class SkNativeStaticReadOnlyProperty<T>(
    override val name: String,
    override val nullable: Boolean,
    override val codec: SkCodec<T>,
    override val getter: () -> T
) : SkNativeStaticProperty<T>() {
    override val readOnly: Boolean get() = true

    override suspend fun setValue(value: SkValue, env: SkriptEnv) {
        typeError("Can't set read-only property $name")
    }
}