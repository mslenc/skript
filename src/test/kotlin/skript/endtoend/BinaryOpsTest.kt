package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.opcodes.compare.compare
import skript.opcodes.equals.aboutEqual
import skript.opcodes.equals.strictlyEqual
import skript.runScriptWithEmit
import skript.values.*

fun test(expr: String, expect: SkValue?): DynamicTest {
    return DynamicTest.dynamicTest(expr) {
        val outputs = runBlocking {
            try {
                runScriptWithEmit("emit($expr);")
            } catch (e: Exception) {
                if (expect != null) {
                    throw e
                } else {
                    null
                }
            }
        }

        assertEquals(expect == null, outputs == null)

        if (expect != null && outputs != null) {
            assertEmittedEquals(listOf(expect), outputs)
        }
    }
}

class BinaryOpsTest {
    @TestFactory
    fun testAddition(): List<DynamicTest> = listOf(
        test(""" 2 + 5       """, 7.toSkript()),
        test(""" 2 + "5"     """, 7.toSkript()),
        test(""" 2 + true    """, 3.toSkript()),
        test(""" 2 + false   """, 2.toSkript()),
        test(""" 2 + "abc"   """, null),
        test(""" 2.5 + 3.5d  """, 6.toSkript()),
        test(""" 2.4d + 3.5d """, "5.9".toBigDecimal().toSkript()),
        test(""" 2.4d + 3.5  """, 5.9.toSkript()),
        test(""" 3.3 + true  """, 4.3.toSkript()),
        test(""" "2.1" + "2.3" + "1.5" """, "5.9".toBigDecimal().toSkript()),
        test(""" true + true + false + true""", 3.toSkript())
    )

    @TestFactory
    fun testSubtraction(): List<DynamicTest> = listOf(
        test(""" 2 - 5       """, (-3).toSkript()),
        test(""" 2 - "5"     """, (-3).toSkript()),
        test(""" 2 - "abc"   """, null),
        test(""" -2 - true    """, (-3).toSkript()),
        test(""" 2.5 - 3.5d  """, (-1).toSkript()),
        test(""" 2.4d - 3.5d """, "-1.1".toBigDecimal().toSkript()),
        test(""" 2.4d - 3.5  """, (-1.1).toSkript()),
        test(""" "-12" - true""", (-13).toSkript()),
        test(""" "2.1" - "2.3" - "1.5" """, "-1.7".toBigDecimal().toSkript()),
        test(""" true - true - false - true""", (-1).toSkript())
    )

    @TestFactory
    fun testMultiplication(): List<DynamicTest> = listOf(
        test(""" 2 * 5       """, 10.toSkript()),
        test(""" 2 * "5"     """, 10.toSkript()),
        test(""" 2 * "abc"   """, null),
        test(""" -2 * true    """, (-2).toSkript()),
        test(""" 2 * false   """, 0.toSkript()),
        test(""" 2.5 * 3.5d  """, (8.75).toSkript()),
        test(""" 2.4d * 3.5d """, "8.4".toBigDecimal().toSkript()),
        test(""" 2.4d * 3.5  """, 8.4.toSkript()),
        test(""" "-12" * true""", (-12).toSkript()),
        test(""" "2.1" * "2.3" * "1.5" """, "7.245".toBigDecimal().toSkript()),
        test(""" true * true * true""", 1.toSkript()),
        test(""" true * true * false * true""", 0.toSkript())
    )

    @TestFactory
    fun testDivision(): List<DynamicTest> = listOf(
        test(""" 2 / 5       """, 0.4.toSkript()),
        test(""" 2 / "5"     """, 0.4.toSkript()),
        test(""" 2 / "abc"   """, null),
        test(""" -2 / true    """, (-2).toSkript()),
        test(""" 2 / false   """, SkUndefined),
        test(""" 2.5 / 3.5d  """, (2.5 / 3.5).toSkript()),
        test(""" 2.4d / 3.5d """, ("2.4".toBigDecimal() / "3.5".toBigDecimal()).toSkript()),
        test(""" 2.4d / 3.5  """, (2.4 / 3.5).toSkript()),
        test(""" "-12" / true""", (-12).toSkript()),
        test(""" "2.1" / "2.3" / "1.5" """, ("2.1".toBigDecimal() / "2.3".toBigDecimal() / "1.5".toBigDecimal()).toSkript()),
        test(""" true / true / true""", 1.toSkript()),
        test(""" true / true / false / true""", SkUndefined)
    )

    @TestFactory
    fun testIntegerDivision(): List<DynamicTest> = listOf(
        test(""" 5 // 2       """, 2.toSkript()),
        test(""" "5" // 2     """, 2.toSkript()),
        test(""" 2 // "abc"   """, null),
        test(""" -2 // true    """, (-2).toSkript()),
        test(""" 2 // false   """, SkUndefined),
        test(""" 3.5d // 2.5  """, 1.toSkript()),
        test(""" 3.5d // 2.5d """, 1.toBigDecimal().toSkript()),
        test(""" 2.4d // 3.5  """, 0.toSkript()),
        test(""" "-12" // true""", (-12).toSkript()),
        test(""" "22.1" // "2.3" // "1.5" """, 6.toBigDecimal().toSkript()),
        test(""" true // true // true""", 1.toSkript()),
        test(""" true // true // false // true""", SkUndefined)
    )

    @TestFactory
    fun testDivisionRemainder(): List<DynamicTest> = listOf(
        test(""" 5 % 2       """, 1.toSkript()),
        test(""" "5" % 2     """, 1.toSkript()),
        test(""" 2 % "abc"   """, null),
        test(""" -2 % true    """, 0.toSkript()),
        test(""" 2 % false   """, SkUndefined),
        test(""" 3.5d % 2.5  """, 1.toSkript()),
        test(""" 3.5d % 2.5d """, 1.toBigDecimal().toSkript()),
        test(""" 2.4d % 3.5  """, 2.4.toSkript()),
        test(""" "-12" % true""", 0.toSkript()),
        test(""" "22.1" % "2.3" % "1.5" """, 1.4.toBigDecimal().toSkript()),
        test(""" true % true """, 0.toSkript()),
        test(""" true % true % true""", 0.toSkript()),
        test(""" true % true % false""", SkUndefined),
        test(""" true % true % false % true""", SkUndefined)
    )

    val equalLists = listOf(
        listOf(SkNull, SkUndefined),
        listOf(SkDecimal.MINUS_ONE, SkDouble.MINUS_ONE, SkString("-1"), SkNumberObject(SkDecimal.MINUS_ONE), SkNumberObject(SkDouble.MINUS_ONE), SkStringObject(SkString("-1"))),
        listOf(SkDecimal.ZERO, SkDouble.ZERO, SkString("0"), SkNumberObject(SkDecimal.ZERO), SkNumberObject(SkDouble.ZERO), SkStringObject(SkString("0"))),
        listOf(SkDecimal.ONE, SkDouble.ONE, SkString("1"), SkNumberObject(SkDecimal.ONE), SkNumberObject(SkDouble.ONE), SkStringObject(SkString("1"))),
        listOf(SkDecimal.valueOf(432), SkDouble.valueOf(432), SkString("432"), SkNumberObject(SkDecimal.valueOf(432)), SkNumberObject(SkDouble.valueOf(432)), SkStringObject(SkString("432"))),
        listOf(SkString("qweqwe"), SkStringObject(SkString("qweqwe"))),
        listOf(SkBoolean.TRUE, SkBooleanObject.TRUE),
        listOf(SkBoolean.FALSE, SkBooleanObject.FALSE),
        listOf(SkList(listOf(SkString("abc"), SkDouble.valueOf(15.0))), SkList(listOf(SkStringObject(SkString("abc")), SkNumberObject(SkDecimal.valueOf("15.0".toBigDecimal()))))),
        listOf(SkMap(mapOf("str" to SkString("abc"), "num" to SkDouble.valueOf(15.0))), SkMap(mapOf("str" to SkStringObject(SkString("abc")), "num" to SkNumberObject(SkDecimal.valueOf("15.0".toBigDecimal()))))),
        listOf(SkList()),
        listOf(SkMap())
    )

    @TestFactory
    fun testEquals(): List<DynamicTest> {
        val result = ArrayList<DynamicTest>()

        for (list1 in equalLists) {
            for (list2 in equalLists) {
                for (el1 in list1) {
                    for (el2 in list2) {
                        assertEquals(list1 === list2, aboutEqual(el1, el2))

                        if (el1 is SkScalar && el2 is SkScalar) {
                            val sb = StringBuilder()
                            el1.toString(sb)
                            sb.append(" == ")
                            el2.toString(sb)

                            result += test(sb.toString(), SkBoolean.valueOf(list1 === list2))
                        }
                    }
                }
            }
        }

        return result
    }

    @TestFactory
    fun testNotEquals(): List<DynamicTest> {
        val result = ArrayList<DynamicTest>()

        for (list1 in equalLists) {
            for (list2 in equalLists) {
                for (el1 in list1) {
                    for (el2 in list2) {
                        if (el1 is SkScalar && el2 is SkScalar) {
                            val sb = StringBuilder()
                            el1.toString(sb)
                            sb.append(" != ")
                            el2.toString(sb)

                            result += test(sb.toString(), SkBoolean.valueOf(list1 !== list2))
                        }
                    }
                }
            }
        }

        return result
    }

    val strictEqualLists = listOf(
        listOf(SkNull),
        listOf(SkUndefined),
        listOf(SkDecimal.MINUS_ONE),
        listOf(SkDouble.MINUS_ONE),
        listOf(SkString("-1")),
        listOf(SkNumberObject(SkDecimal.MINUS_ONE)),
        listOf(SkNumberObject(SkDouble.MINUS_ONE)),
        listOf(SkStringObject(SkString("-1"))),
        listOf(SkDecimal.ZERO),
        listOf(SkDouble.ZERO),
        listOf(SkString("0")),
        listOf(SkNumberObject(SkDecimal.ZERO)),
        listOf(SkNumberObject(SkDouble.ZERO)),
        listOf(SkStringObject(SkString("0"))),
        listOf(SkDecimal.ONE),
        listOf(SkDouble.ONE),
        listOf(SkString("1")),
        listOf(SkNumberObject(SkDecimal.ONE)),
        listOf(SkNumberObject(SkDouble.ONE)),
        listOf(SkStringObject(SkString("1"))),
        listOf(SkDecimal.valueOf(432)),
        listOf(SkDouble.valueOf(432)),
        listOf(SkString("432")),
        listOf(SkNumberObject(SkDecimal.valueOf(432))),
        listOf(SkNumberObject(SkDouble.valueOf(432))),
        listOf(SkStringObject(SkString("432"))),
        listOf(SkString("qweqwe")),
        listOf(SkStringObject(SkString("qweqwe"))),
        listOf(SkBoolean.TRUE),
        listOf(SkBooleanObject.TRUE),
        listOf(SkBoolean.FALSE),
        listOf(SkBooleanObject.FALSE),
        listOf(SkList(listOf(SkString("abc"), SkDouble.valueOf(15.0)))),
        listOf(SkList(listOf(SkStringObject(SkString("abc")), SkNumberObject(SkDecimal.valueOf("15.0".toBigDecimal()))))),
        listOf(SkMap(mapOf("str" to SkString("abc"), "num" to SkDouble.valueOf(15.0)))),
        listOf(SkMap(mapOf("str" to SkStringObject(SkString("abc")), "num" to SkNumberObject(SkDecimal.valueOf("15.0".toBigDecimal()))))),
        listOf(SkList()),
        listOf(SkMap())
    )

    @TestFactory
    fun testStrictEquals(): List<DynamicTest> {
        val result = ArrayList<DynamicTest>()

        for (list1 in strictEqualLists) {
            for (list2 in strictEqualLists) {
                for (el1 in list1) {
                    for (el2 in list2) {
                        assertEquals(list1 === list2, strictlyEqual(el1, el2))

                        if (el1 is SkScalar && el2 is SkScalar) {
                            val sb = StringBuilder()
                            el1.toString(sb)
                            sb.append(" === ")
                            el2.toString(sb)

                            result += test(sb.toString(), SkBoolean.valueOf(list1 === list2))
                        }
                    }
                }
            }
        }

        return result
    }

    @TestFactory
    fun testNotStrictlyEqual(): List<DynamicTest> {
        val result = ArrayList<DynamicTest>()

        for (list1 in strictEqualLists) {
            for (list2 in strictEqualLists) {
                for (el1 in list1) {
                    for (el2 in list2) {
                        if (el1 is SkScalar && el2 is SkScalar) {
                            val sb = StringBuilder()
                            el1.toString(sb)
                            sb.append(" !== ")
                            el2.toString(sb)

                            result += test(sb.toString(), SkBoolean.valueOf(list1 !== list2))
                        }
                    }
                }
            }
        }

        return result
    }

    @Test
    fun testStrictImpliesAboutEqual() {
        for (list1 in equalLists + strictEqualLists) {
            for (list2 in equalLists + strictEqualLists) {
                for (el1 in list1) {
                    for (el2 in list2) {
                        if (strictlyEqual(el1, el2)) {
                            assertTrue(aboutEqual(el1, el2))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testNotEqualImpliesNotStrictlyEqual() {
        for (list1 in equalLists + strictEqualLists) {
            for (list2 in equalLists + strictEqualLists) {
                for (el1 in list1) {
                    for (el2 in list2) {
                        if (!aboutEqual(el1, el2)) {
                            assertFalse(strictlyEqual(el1, el2))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testDeepEqualsLists() = runBlocking {
        val outputs = runScriptWithEmit("""
            val a = [ 71, 2, 3 ];
            val b = [ 71, 2, 3 ];
            val c = [ 3, 2, 3 ];
            
            a.push(b);
            b.push(a);
            c.push(a);
            
            emit(a[3][3][3][3][3][3][3][3][3][0]);
            
            emit(a == b);
            emit(a == c);
            emit(b == c);
            
            emit(b == a);
            emit(c == a);
            emit(c == b);
        
        
            val d = { a: 12, b: 23, c: 34 };
            val e = { a: 12, b: 23, c: 34, d: d };
            d.d = e;
            val f = { a: 12, b: 23, c: 34, d: 45 };
            val g = { a: 12 };

            emit(d.d.d.d.d.d.d.d.d.d.d.d.d.d.d.d.d.c);
            
            emit(d == e);
            emit(d == f);
            emit(e == f);
            emit(e == g);
            emit(g == g);
            
            emit(a == d);
            emit(a == e);
            emit(a == f);
            emit(b == d);
            emit(b == e);
            emit(b == f);
            emit(c == d);
            emit(c == e);
            emit(c == f);
        """.trimIndent())

        val expect = listOf(
            71.toSkript(),

            true.toSkript(),
            false.toSkript(),
            false.toSkript(),

            true.toSkript(),
            false.toSkript(),
            false.toSkript(),

            34.toSkript(),

            true.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            true.toSkript(),

            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript(),
            false.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    val compareSets = listOf(
        // numbers
        listOf(
            listOf(SkDouble.valueOf(-5), SkDecimal.valueOf("-5".toBigDecimal())),
            listOf(SkDouble.valueOf(-3.5), SkDecimal.valueOf("-3.5".toBigDecimal()), SkString("-3.5")),
            listOf(SkDouble.valueOf(-1), SkDecimal.valueOf("-1".toBigDecimal())),
            listOf(SkDouble.ZERO, SkDecimal.valueOf("0".toBigDecimal())),
            listOf(SkDouble.valueOf(0.25), SkDecimal.valueOf("0.25".toBigDecimal()), SkString("0.25")),
            listOf(SkDouble.valueOf(1), SkDecimal.valueOf("1".toBigDecimal())),
            listOf(SkDouble.valueOf(12.125), SkDecimal.valueOf("12.125".toBigDecimal()))
        ),

        // booleans
        listOf(
            listOf(SkBoolean.FALSE, SkBooleanObject.FALSE),
            listOf(SkBoolean.TRUE, SkBooleanObject.TRUE)
        ),

        // strings
        listOf(
            listOf(SkString("abc"), SkStringObject(SkString("abc"))),
            listOf(SkString("abcd"), SkStringObject(SkString("abcd"))),
            listOf(SkString("cde"), SkStringObject(SkString("cde")))
        )
    )

    @TestFactory
    fun testCompare(): List<DynamicTest> {
        val result = ArrayList<DynamicTest>()

        for (type in compareSets) {
            for (i1 in type.indices) {
                for (i2 in type.indices) {
                    for (value1 in type[i1]) {
                        for (value2 in type[i2]) {
                            if (value1 === value2) {
                                assertTrue(compare(value1, value2) == 0)
                            } else {
                                val expect = i1.compareTo(i2)
                                assertEquals(expect, compare(value1, value2)) { "$value1 <=> $value2" }
                            }

                            if (value1 is SkScalar && value2 is SkScalar) {
                                val sb = StringBuilder()
                                value1.toString(sb)
                                sb.append(" <=> ")
                                value2.toString(sb)
                                val testSource = sb.toString()

                                val expected = when (compare(value1, value2)) {
                                    null -> SkUndefined
                                    -1 -> SkDouble.MINUS_ONE
                                    0 -> SkDouble.ZERO
                                    1 -> SkDouble.ONE
                                    else -> throw IllegalStateException("compare should only return null, -1, 0 or 1")
                                }

                                result += test(testSource, expected)

                                val expectedLessThan = when (compare(value1, value2)) {
                                    null -> SkUndefined
                                    -1 -> SkBoolean.TRUE
                                    0 -> SkBoolean.FALSE
                                    1 -> SkBoolean.FALSE
                                    else -> throw IllegalStateException("compare should only return null, -1, 0 or 1")
                                }

                                result += test(testSource.replace(" <=> ", " < "), expectedLessThan)

                                val expectedLessOrEqual = when (compare(value1, value2)) {
                                    null -> SkUndefined
                                    -1 -> SkBoolean.TRUE
                                    0 -> SkBoolean.TRUE
                                    1 -> SkBoolean.FALSE
                                    else -> throw IllegalStateException("compare should only return null, -1, 0 or 1")
                                }

                                result += test(testSource.replace(" <=> ", " <= "), expectedLessOrEqual)

                                val expectedGreaterOrEqual = when (compare(value1, value2)) {
                                    null -> SkUndefined
                                    -1 -> SkBoolean.FALSE
                                    0 -> SkBoolean.TRUE
                                    1 -> SkBoolean.TRUE
                                    else -> throw IllegalStateException("compare should only return null, -1, 0 or 1")
                                }

                                result += test(testSource.replace(" <=> ", " >= "), expectedGreaterOrEqual)

                                val expectedGreaterThan = when (compare(value1, value2)) {
                                    null -> SkUndefined
                                    -1 -> SkBoolean.FALSE
                                    0 -> SkBoolean.FALSE
                                    1 -> SkBoolean.TRUE
                                    else -> throw IllegalStateException("compare should only return null, -1, 0 or 1")
                                }

                                result += test(testSource.replace(" <=> ", " > "), expectedGreaterThan)
                            }
                        }
                    }
                }
            }
        }

        return result
    }

    @Test
    fun testCompareChainBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            emit(1 < 2 < 3 < 4);
            emit(1 <= 2 <= 3 <= 4);
            emit(1 >= 2 >= 3 >= 4);
            emit(1 > 2 > 3 > 4);
            
            emit(1 <= 1 <= 2);
            emit(1 < 1 < 2);
            emit(1 == 1 < 2);
            
            emit(1 > 2 > 3 > 4);
            emit(1 >= 2 >= 3 >= 4);
            emit(4 > 3 > 2 > 1);
            emit(4 >= 3 >= 2 >= 1);
            emit(4 >= 3 == 3 >= 2 == 2 >= 1 == 1);
        """.trimIndent())

        val expect = listOf(
            true.toSkript(),
            true.toSkript(),
            false.toSkript(),
            false.toSkript(),

            true.toSkript(),
            false.toSkript(),
            true.toSkript(),

            false.toSkript(),
            false.toSkript(),
            true.toSkript(),
            true.toSkript(),
            true.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testCompareChainNotEqual() = runBlocking {
        val outputs = runScriptWithEmit("""
            emit(1 != 2 != 3 != 1);
            emit(1 != 2 != 3);
            
            emit(1 != 2 != 3 != "1");
            emit(1 != 2 != 3 != "abc");
            
            emit([ 1, 2 ] != [ 2, 3 ] != [ 1, 2 ]);
            emit([ 1, 2 ] != [ 2, 3 ] != [ 3, 4 ]);
        """.trimIndent())

        val expect = listOf(
            false.toSkript(),
            true.toSkript(),

            false.toSkript(),
            true.toSkript(),

            false.toSkript(),
            true.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testCompareChainNotStrictlyEqual() = runBlocking {
        val outputs = runScriptWithEmit("""
            emit(1 !== 2 !== 3 !== 1);
            emit(1 !== 2 !== 3);
            
            emit(1 !== 2 !== 3 !== "1");
            emit(1 !== 2 !== 3 !== "abc");

            emit([ 1, 2 ] !== [ 2, 3 ] !== [ 1, 2 ]);
            emit([ 1, 2 ] !== [ 2, 3 ] !== [ 3, 4 ]);
        """.trimIndent())

        val expect = listOf(
            false.toSkript(),
            true.toSkript(),

            true.toSkript(),
            true.toSkript(),

            true.toSkript(),
            true.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }
}

/*

    OR("|"),
    AND("&"),
    OR_OR("||"),
    AND_AND("&&"),

    ELVIS("?:"),

    RANGE_TO(".."),
    RANGE_TO_EXCL("..<")*/