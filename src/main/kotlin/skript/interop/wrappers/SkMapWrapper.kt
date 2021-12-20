package skript.interop.wrappers

import kotlinx.coroutines.runBlocking
import skript.interop.ConversionType
import skript.interop.HoldsNative
import skript.interop.SkCodec
import skript.io.SkriptEnv
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.typeError
import skript.values.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class SkMapWrapper<K, V>(override val nativeObj: Map<K, V>, val keyCodec: SkCodec<K>, val valueCodec: SkCodec<V>, val env: SkriptEnv) : SkAbstractMap(), HoldsNative<Map<K, V>> {
    override val klass: SkMapWrapperClassDef
        get() = SkMapWrapperClassDef

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun unwrap(): Map<K, V> {
        return nativeObj
    }

    override suspend fun makeIterator(): SkIterator {
        return SkNativeMapIterator(this, env)
    }

    override suspend fun entryGet(key: SkValue, env: SkriptEnv): SkValue {
        return if (keyCodec.canConvert(key) != ConversionType.NOT_POSSIBLE) {
            val nativeKey = keyCodec.toKotlin(key, env)
            if (nativeObj.containsKey(nativeKey)) {
                val nativeValue = nativeObj.get(nativeKey)
                if (nativeValue != null) {
                    valueCodec.toSkript(nativeValue, env)
                } else {
                    SkNull
                }
            } else {
                SkUndefined
            }
        } else {
            SkUndefined
        }
    }

    override suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv) {
        // TODO
        typeError("No support for mutable native maps (yet)")
    }

    override suspend fun entryDelete(key: SkValue, env: SkriptEnv): Boolean {
        // TODO
        typeError("No support for mutable native maps (yet)")
    }
}

object SkMapWrapperClassDef : SkCustomClass<SkMapWrapper<*,*>>("MapWrapper", SkAbstractMapClassDef)

class SkCodecNativeMap<K, V>(val mapClass: KClass<*>, val keyCodec: SkCodec<K>, val valueCodec: SkCodec<V>) : SkCodec<Map<K, V>> {
    override fun canConvert(value: SkValue): ConversionType {
        if (value is SkMapWrapper<*,*> && value.keyCodec == keyCodec && value.valueCodec == valueCodec)
            if (mapClass.isInstance(value.nativeObj))
                return ConversionType.EXACT

        if (value is SkAbstractMap) {
            return when {
                mapClass.isSuperclassOf(LinkedHashMap::class) ->
                    ConversionType.COERCE
                else ->
                    ConversionType.NOT_POSSIBLE
            }
        }

        return ConversionType.NOT_POSSIBLE
    }

    override fun toKotlin(value: SkValue, env: SkriptEnv): Map<K, V> {
        if (value is SkMapWrapper<*,*> && value.keyCodec == keyCodec && value.valueCodec == valueCodec)
            if (mapClass.isInstance(value.nativeObj))
                return value.nativeObj as Map<K, V>

        if (value is SkAbstractMap) {
            val container: MutableMap<K, V> = when {
                mapClass.isSuperclassOf(HashMap::class) -> LinkedHashMap()
                else -> typeError("Can't convert $value to $mapClass")
            }

            val iterator = runBlocking { value.makeIterator() }
            if (iterator != null) {
                while (iterator.moveToNext()) {
                    val nativeKey = keyCodec.toKotlin(iterator.getCurrentKey(), env)
                    val nativeValue = valueCodec.toKotlin(iterator.getCurrentValue(), env)
                    container[nativeKey] = nativeValue
                }
            } else {
                typeError("Missing iterator when converting map.")
            }

            return container
        }

        typeError("Can't convert $value to $mapClass")
    }

    override fun toSkript(value: Map<K, V>, env: SkriptEnv): SkValue {
        return SkMapWrapper(value, keyCodec, valueCodec, env)
    }
}

class SkNativeMapIterator<K, V>(val map: SkMapWrapper<K, V>, env: SkriptEnv) : SkIterator() {
    override val klass: SkClassDef
        get() = SkNativeMapIteratorClassDef

    var pos = -1
    val entriesCopy: List<Pair<SkValue, SkValue>> = ArrayList<Pair<SkValue, SkValue>>().apply {
        map.nativeObj.forEach { (key, value) ->
            add(map.keyCodec.toSkript(key, env) to map.valueCodec.toSkript(value, env))
        }
    }

    override fun moveToNext(): Boolean {
        return ++pos < entriesCopy.size
    }

    override fun getCurrentKey(): SkValue {
        return entriesCopy[pos].first
    }

    override fun getCurrentValue(): SkValue {
        return entriesCopy[pos].second
    }
}

object SkNativeMapIteratorClassDef : SkClassDef("NativeMapIterator", SkIteratorClassDef)