package skript.interop.wrappers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.interop.SkCodec
import skript.interop.SkCodecDouble
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkDoubleArray(override val nativeObj: DoubleArray) : SkAbstractNativeArray<DoubleArray>() {
    override val klass: SkClassDef
        get() = SkDoubleArrayClassDef

    override val elementCodec: SkCodec<*>
        get() = SkCodecDouble

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecDouble.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkDoubleArrayIterator(nativeObj)
    }

    override fun unwrap(): DoubleArray {
        return nativeObj
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        val array = factory.arrayNode(nativeObj.size)

        for (value in nativeObj)
            array.add(value)

        return array
    }
}

object SkDoubleArrayClassDef : SkClassDef("DoubleArray", SkAbstractListClassDef)

object SkCodecDoubleArray : SkCodecTypedArray<DoubleArray, Double>() {
    override val elementCodec: SkCodec<Double>
        get() = SkCodecDouble

    override fun createArray(size: Int): DoubleArray {
        return DoubleArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is DoubleArray
    }

    override fun setElement(array: DoubleArray, index: Int, value: Double) {
        array[index] = value
    }

    override fun toSkript(value: DoubleArray, env: SkriptEnv) = SkDoubleArray(value)
}

class SkDoubleArrayIterator(val array: DoubleArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkDoubleArrayIteratorClassDef

    var pos = -1

    override fun moveToNext(): Boolean {
        return ++pos < array.size
    }

    override fun getCurrentKey(): SkValue {
        return SkNumber.valueOf(pos)
    }

    override fun getCurrentValue(): SkValue {
        return array[pos].toSkript()
    }
}

object SkDoubleArrayIteratorClassDef : SkClassDef("DoubleArrayIterator", SkIteratorClassDef)