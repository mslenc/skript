package skript.interop

import skript.values.SkObject
import skript.values.SkString
import skript.values.SkValue

class SkNativeObject<T>(val nativeObj: T, override val klass: SkNativeClass<T>) : SkObject() {
    override suspend fun setMember(key: String, value: SkValue) {
        klass.properties[key]?.let { property ->
            property.setValue(nativeObj, value)
            return
        }

        super.setMember(key, value)
    }

    override suspend fun findMember(key: SkValue): SkValue {
        (key as? SkString)?.let {
            klass.properties[it.value]?.let { property ->
                return property.getValue(nativeObj)
            }
        }

        return super.findMember(key)
    }

    override suspend fun hasOwnMember(key: SkValue): Boolean {
        (key as? SkString)?.let {
            klass.properties[it.value]?.let {
                return true
            }
        }

        return super.hasOwnMember(key)
    }


}