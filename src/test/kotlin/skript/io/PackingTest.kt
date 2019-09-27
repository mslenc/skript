package skript.io

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.opcodes.equals.deepEquals
import skript.opcodes.equals.strictlyEqual
import skript.values.*

class PackingTest {
    @Test
    fun testScalars() {
        val tests = listOf(
            SkNumber.valueOf(0),
            SkNull,
            SkUndefined,
            SkString.EMPTY,
            SkString.FALSE,
            SkString("A somewhat longer string, not entirely fitting in 9 characters"),
            SkBoolean.TRUE,
            SkBoolean.FALSE,
            SkNumber.valueOf(123),
            SkNumber.valueOf(Math.PI),
            SkNumber.valueOf("1249829485739284239587293857293.2384972948573249".toBigDecimal())
        )

        for (value in tests) {
            val unpacked = unpack(pack(value))
            assertTrue(strictlyEqual(unpacked, value)) { value.toString() }
        }
    }

    @Test
    fun testSimpleLists() {
        val list1 = SkList()

        val list2 = SkList()
        list2.add(SkNumber.valueOf(3))
        list2.add(SkNumber.valueOf(1))
        list2.add(SkNumber.valueOf(4))
        list2.add(SkNumber.valueOf(2))

        val list3 = SkList()
        for (i in 1..30) {
            repeat(i) { // to test the RLE
                list3.add(SkNumber.valueOf(i))
            }
        }

        val list4 = SkList()
        list4.add(list2)
        list4.add(list2)
        list4.add(list2)
        list4.add(list2)

        val list5 = SkList()
        list5.add(true.toSkript())

        val tests = listOf(list1, list2, list3, list4, list5)

        for (list in tests) {
            val packed = pack(list)
            println("$list -> $packed")
            val unpacked = unpack(packed) as SkList

            assertTrue(deepEquals(unpacked, list, HashSet(), strictElementEqual = true))
        }
    }

    @Test
    fun testSimpleMaps() {
        val map1 = SkMap()

        val map2 = SkMap()
        map2.entries["abc"] = SkDecimal.MINUS_ONE

        val map3 = SkMap()
        map3.entries["map2"] = map2
        map3.entries["list"] = SkList(listOf(
            SkString.FALSE,
            SkString("A somewhat longer string"),
            SkBoolean.TRUE,
            map2
        ))


        val tests = listOf(map1, map2, map3)

        for (list in tests) {
            val packed = pack(list)
            println("$list -> $packed")
            val unpacked = unpack(packed) as SkMap

            assertTrue(deepEquals(unpacked, list, HashSet(), strictElementEqual = true))
        }
    }
}