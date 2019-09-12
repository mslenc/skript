package skript.values

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.io.toSkript
import skript.opcodes.SkIterator

class SkNumberRangeTest {
    @Test
    fun testNumberRangeBasicsInclusive() = runBlocking {
        val range = SkNumberRange(1.toSkript(), 6.toSkript(), true)

        assertFalse(range.hasOwnMemberInternal(0.toSkript()))
        assertTrue(range.hasOwnMemberInternal(1.toSkript()))
        assertTrue(range.hasOwnMemberInternal(2.toSkript()))
        assertTrue(range.hasOwnMemberInternal(5.9.toSkript()))
        assertTrue(range.hasOwnMemberInternal(6.toSkript()))
        assertFalse(range.hasOwnMemberInternal(6.1.toSkript()))

        val iter = range.makeIterator()

        assertTrue(iter.moveToNext())
        assertEquals(0.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(1.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(1.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(2.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(2.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(3.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(3.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(4.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(4.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(5.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(5.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(6.0, (iter.getCurrentValue() as SkNumber).value)

        assertFalse(iter.moveToNext())
    }

    @Test
    fun testNumberRangeBasicsExclusive() = runBlocking {
        val range = SkNumberRange(1.toSkript(), 6.toSkript(), false)

        assertFalse(range.hasOwnMemberInternal(0.toSkript()))
        assertTrue(range.hasOwnMemberInternal(1.toSkript()))
        assertTrue(range.hasOwnMemberInternal(2.toSkript()))
        assertTrue(range.hasOwnMemberInternal(5.999999.toSkript()))
        assertFalse(range.hasOwnMemberInternal(6.toSkript()))
        assertFalse(range.hasOwnMemberInternal(6.1.toSkript()))

        val iter = range.makeIterator() as SkIterator

        assertTrue(iter.moveToNext())
        assertEquals(0.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(1.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(1.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(2.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(2.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(3.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(3.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(4.0, (iter.getCurrentValue() as SkNumber).value)

        assertTrue(iter.moveToNext())
        assertEquals(4.0, (iter.getCurrentKey() as SkNumber).value)
        assertEquals(5.0, (iter.getCurrentValue() as SkNumber).value)

        assertFalse(iter.moveToNext())
    }
}