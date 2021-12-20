package skript.interop.wrappers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import skript.interop.SkCodec
import skript.interop.SkCodecByte
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkByteArray(override val nativeObj: ByteArray) : SkAbstractNativeArray<ByteArray>() {
    override val klass: SkClassDef
        get() = SkByteArrayClassDef

    override val elementCodec: SkCodec<*>
        get() = SkCodecByte

    override fun getSize(): Int {
        return nativeObj.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return nativeObj[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        nativeObj[index] = SkCodecByte.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkByteArrayIterator(nativeObj)
    }

    override fun unwrap(): ByteArray {
        return nativeObj
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        return factory.binaryNode(nativeObj)
    }
}

object SkByteArrayClassDef : SkClassDef("ByteArray", SkAbstractListClassDef)

object SkCodecByteArray : SkCodecTypedArray<ByteArray, Byte>() {
    override val elementCodec: SkCodec<Byte>
        get() = SkCodecByte

    override fun createArray(size: Int): ByteArray {
        return ByteArray(size)
    }

    override fun isArrayInstance(obj: Any): Boolean {
        return obj is ByteArray
    }

    override fun setElement(array: ByteArray, index: Int, value: Byte) {
        array[index] = value
    }

    override fun toSkript(value: ByteArray, env: SkriptEnv) = SkByteArray(value)
}

class SkByteArrayIterator(val array: ByteArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkByteArrayIteratorClassDef

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

object SkByteArrayIteratorClassDef : SkClassDef("ByteArrayIterator", SkIteratorClassDef)