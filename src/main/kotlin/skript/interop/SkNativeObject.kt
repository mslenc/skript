package skript.interop

import skript.exec.RuntimeState
import skript.values.SkObject
import skript.values.SkString
import skript.values.SkValue

class SkNativeObject<T: Any>(val nativeObj: T, override val klass: SkNativeClassDef<T>) : SkObject() {
    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        (key as? SkString)?.let {
            klass.properties[it.value]?.let { property ->
                property.setValue(nativeObj, value, state.env)
                return
            }
        }

        super.setMember(key, value, state)
    }

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        (key as? SkString)?.let {
            klass.properties[it.value]?.let { property ->
                return property.getValue(nativeObj, state.env)
            }
        }

        return super.findMember(key, state)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        (key as? SkString)?.let {
            klass.properties[it.value]?.let {
                return true
            }
        }

        return super.hasOwnMember(key, state)
    }
}