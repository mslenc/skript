package skript.values

import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.toStrictNumberOrNull
import skript.util.ArgsExtractor
import skript.util.expectBoolean
import skript.util.expectNumber
import java.math.BigDecimal

class SkNumberRange(val start: SkNumber, val end: SkNumber, val endInclusive: Boolean) : SkObject() {
    override val klass: SkClass
        get() = SkNumberRangeClass

    override suspend fun makeIterator(): SkValue {
        return if (start is SkDecimal && end is SkDecimal) {
            SkDecimalRangeIterator(start, end, endInclusive)
        } else {
            SkDoubleRangeIterator(start.toDouble(), end.toDouble(), endInclusive)
        }
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

class SkDoubleRangeIterator(start: Double, val end: Double, val endInclusive: Boolean) : SkObject(), SkIterator {
    override val klass: SkClass
        get() = SkNumberRangeIteratorClass

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

object SkNumberRangeIteratorClass : SkClass("NumberRangeIterator", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        throw IllegalStateException("This should never be called")
    }
}

class SkDecimalRangeIterator(start: SkDecimal, val end: SkDecimal, val endInclusive: Boolean) : SkObject(), SkIterator {
    override val klass: SkClass
        get() = SkDecimalRangeIteratorClass

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

object SkDecimalRangeIteratorClass : SkClass("DecimalRangeIteratorClass", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        throw IllegalStateException("This should never be called")
    }
}