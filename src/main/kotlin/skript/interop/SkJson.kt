package skript.interop

import skript.io.SkriptIgnore
import skript.values.*

class SkJson @SkriptIgnore constructor(@property:SkriptIgnore val codec: JsonCodec<*> = JsonCodec.DEFAULT) {

    suspend fun parse(json: String): SkValue {
        return codec.fromJsonString(json)
    }

    suspend fun stringify(value: SkValue): String {
        return codec.toJsonString(value)
    }
}