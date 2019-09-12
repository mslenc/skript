package skript.interop

import skript.values.SkObject

class SkNativeObject<T: Any>(val nativeObj: T, override val klass: SkNativeClassDef<T>) : SkObject()