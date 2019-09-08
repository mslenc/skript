package skript.interop

import skript.typeError
import skript.values.SkNull
import skript.values.SkValue
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

sealed class SkNativeProperty<RCVR, T> {
    abstract val name: String
    abstract val property: KProperty1<RCVR, T>
    abstract val codec: SkCodec<T>
    abstract val nullable: Boolean
    abstract val readOnly: Boolean

    fun getValue(nativeObject: RCVR): SkValue {
        return when (val nativeVal = property.get(nativeObject)) {
            null -> SkNull
            else -> codec.toSkript(nativeVal)
        }
    }

    abstract fun setValue(nativeObject: RCVR, value: SkValue)
}

class SkNativeMutableProperty<RCVR, T>(
    override val property: KMutableProperty1<RCVR, T>,
    override val codec: SkCodec<T>
) : SkNativeProperty<RCVR, T>() {
    override val name: String
        get() = property.name

    override val readOnly: Boolean
        get() = false

    override val nullable: Boolean
        get() = property.returnType.isMarkedNullable

    override fun setValue(nativeObject: RCVR, value: SkValue) {
        val nativeValue = codec.toKotlin(value)
        property.set(nativeObject, nativeValue)
    }
}

class SkNativeReadOnlyProperty<RCVR, T>(
    override val property: KProperty1<RCVR, T>,
    override val codec: SkCodec<T>
) : SkNativeProperty<RCVR, T>() {
    override val name: String
        get() = property.name

    override val readOnly: Boolean
        get() = true

    override val nullable: Boolean
        get() = property.returnType.isMarkedNullable

    override fun setValue(nativeObject: RCVR, value: SkValue) {
        typeError("Can't set read-only property $name")
    }
}