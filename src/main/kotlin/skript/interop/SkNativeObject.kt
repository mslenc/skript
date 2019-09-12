package skript.interop

import skript.values.SkObject

interface HoldsNative<T: Any> {
    val nativeObj: T
}

class SkNativeObject<T: Any>(override val nativeObj: T, override val klass: SkNativeClassDef<T>) : SkObject(), HoldsNative<T>

