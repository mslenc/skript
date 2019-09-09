package skript.interop

import skript.interop.wrappers.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

private fun <T: Any> MutableMap<KType, SkCodec<*>>.addBoth(klass: KClass<T>, codec: SkCodec<T>) {
    set(klass.createType(nullable = true), codec)
    set(klass.createType(nullable = false), codec)
}

internal fun initCodecs(out: MutableMap<KType, SkCodec<*>>) {
    out.addBoth(String::class, SkCodecString)
    out.addBoth(CharSequence::class, SkCodecCharSequence)

    out.addBoth(Byte::class, SkCodecByte)
    out.addBoth(Short::class, SkCodecShort)
    out.addBoth(Int::class, SkCodecInt)
    out.addBoth(Long::class, SkCodecLong)
    out.addBoth(Float::class, SkCodecFloat)
    out.addBoth(Double::class, SkCodecDouble)
    out.addBoth(Char::class, SkCodecChar)
    out.addBoth(Boolean::class, SkCodecBoolean)

    out.addBoth(ByteArray::class, SkCodecByteArray)
    out.addBoth(ShortArray::class, SkCodecShortArray)
    out.addBoth(IntArray::class, SkCodecIntArray)
    out.addBoth(LongArray::class, SkCodecLongArray)
    out.addBoth(FloatArray::class, SkCodecFloatArray)
    out.addBoth(DoubleArray::class, SkCodecDoubleArray)
    out.addBoth(BooleanArray::class, SkCodecBooleanArray)
    out.addBoth(CharArray::class, SkCodecCharArray)
}