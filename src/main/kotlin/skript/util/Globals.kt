package skript.util

import skript.notSupported
import skript.values.SkUndefined
import skript.values.SkValue

private class GlobalEntry(var value: SkValue, val protected: Boolean)

class Globals {
    private val entries = HashMap<String, GlobalEntry>()

    fun get(key: String): SkValue {
        return entries[key]?.value ?: SkUndefined
    }

    fun set(key: String, value: SkValue, protected: Boolean = false) {
        entries[key]?.let { existing ->
            if (existing.protected)
                throw notSupported("Global $key is protected and can't be changed")

            existing.value = value
            return
        }

        entries[key] = GlobalEntry(value, protected)
    }
}