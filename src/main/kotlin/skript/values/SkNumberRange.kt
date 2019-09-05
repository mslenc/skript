package skript.values

import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.toStrictNumberOrNull
import skript.util.ArgsExtractor
import skript.util.expectBoolean
import skript.util.expectNumber
import java.math.BigDecimal

class SkNumberRange(val start: SkNumber, val end: SkNumber, val endInclusive: Boolean) : SkObject(SkNumberRangeClass) {
    override suspend fun makeIterator(): SkValue {
        return SkNumberRangeIterator(start, end, endInclusive)
    }

    override suspend fun hasOwnMember(key: SkValue): Boolean {
        val num = key.toStrictNumberOrNull() ?: return false

        return when {
            num < start -> false
            endInclusive && num > end -> false
            !endInclusive && num >= end -> false
            else -> true
        }
    }
}

class SkNumberRangeIterator(start: SkNumber, val end: SkNumber, val endInclusive: Boolean) : SkObject(SkNumberRangeIteratorClass), SkIterator {
    var iteration = -1
    var currValue = start

    override fun moveToNext(): Boolean {
        if (++iteration >= 0)
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

object SkNumberRangeClass : SkClass("NumberRange", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val args = ArgsExtractor(posArgs, kwArgs, "NumberRange")

        val start = args.expectNumber("start", coerce = true, ifUndefined = SkNumber.ZERO)
        val end = args.expectNumber("end", coerce = true, ifUndefined = SkNumber.ZERO)
        val endInclusive = args.expectBoolean("endInclusive", coerce = true, ifUndefined = true)
        args.expectNothingElse()

        return SkNumberRange(start, end, endInclusive)
    }
}

object SkNumberRangeIteratorClass : SkClass("NumberRangeIterator", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        throw IllegalStateException("This should never be called")
    }
}