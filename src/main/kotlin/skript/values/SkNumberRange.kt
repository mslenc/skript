package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.mslenc.utils.ComparableRangeEx
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.toStrictNumberOrNull
import skript.util.SkArguments
import skript.util.expectBoolean
import skript.util.expectNumber
import java.math.BigDecimal

class SkNumberRange(val start: SkNumber, val end: SkNumber, val endInclusive: Boolean) : SkObject() {
    override val klass: SkClassDef
        get() = SkNumberRangeClassDef

    override suspend fun makeIterator(): SkIterator {
        return if (start is SkDecimal && end is SkDecimal) {
            SkDecimalRangeIterator(start, end, endInclusive)
        } else {
            SkDoubleRangeIterator(start.toDouble(), end.toDouble(), endInclusive)
        }
    }

    override suspend fun contains(key: SkValue, env: SkriptEnv): Boolean {
        return hasOwnMemberInternal(key.toStrictNumberOrNull() ?: return false)
    }

    fun hasOwnMemberInternal(num: SkNumber): Boolean {
        return when {
            num < start -> false
            endInclusive && num > end -> false
            !endInclusive && num >= end -> false
            else -> true
        }
    }

    override fun unwrap(): Any {
        return if (endInclusive) {
            if (start is SkDecimal && end is SkDecimal) {
                start.unwrap()..end.unwrap()
            } else {
                start.toDouble()..end.toDouble()
            }
        } else {
            if (start is SkDecimal && end is SkDecimal) {
                ComparableRangeEx(start.unwrap(), end.unwrap())
            } else {
                ComparableRangeEx(start.toDouble(), end.toDouble())
            }
        }
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        val list = factory.arrayNode()

        list.add(start.toJson(factory))
        list.add(end.toJson(factory))
        list.add(endInclusive)

        return list
    }
}

object SkNumberRangeClassDef : SkClassDef("NumberRange", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val start = args.expectNumber("start", coerce = true, ifUndefined = SkNumber.ZERO)
        val end = args.expectNumber("end", coerce = true, ifUndefined = SkNumber.ZERO)
        val endInclusive = args.expectBoolean("endInclusive", coerce = true, ifUndefined = true)
        args.expectNothingElse()

        return SkNumberRange(start, end, endInclusive)
    }
}

class SkDoubleRangeIterator(start: Double, val end: Double, val endInclusive: Boolean) : SkIterator() {
    override val klass: SkClassDef
        get() = SkNumberRangeIteratorClassDef

    var iteration = -1
    var currValue = start

    override fun moveToNext(): Boolean {
        if (++iteration > 0)
            currValue += 1.0

        return when {
            endInclusive && currValue > end -> false
            !endInclusive && currValue >= end -> false
            else -> true
        }
    }

    override fun getCurrentKey(): SkDouble {
        return iteration.toSkript()
    }

    override fun getCurrentValue(): SkDouble {
        return currValue.toSkript()
    }
}

object SkNumberRangeIteratorClassDef : SkClassDef("NumberRangeIterator", SkIteratorClassDef)

class SkDecimalRangeIterator(start: SkDecimal, val end: SkDecimal, val endInclusive: Boolean) : SkIterator() {
    override val klass: SkClassDef
        get() = SkDecimalRangeIteratorClassDef

    var iteration = -1
    var currValue = start

    override fun moveToNext(): Boolean {
        if (++iteration > 0)
            currValue = (currValue.value + BigDecimal.ONE).toSkript()

        return when {
            endInclusive && currValue > end -> false
            !endInclusive && currValue >= end -> false
            else -> true
        }
    }

    override fun getCurrentKey(): SkValue {
        return iteration.toSkript()
    }

    override fun getCurrentValue(): SkValue {
        return currValue
    }
}

object SkDecimalRangeIteratorClassDef : SkClassDef("DecimalRangeIteratorClass", SkObjectClassDef)