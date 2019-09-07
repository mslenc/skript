package skript

import org.junit.jupiter.api.Assertions.assertTrue
import skript.opcodes.equals.strictlyEqual
import skript.values.SkValue

inline fun assertStrictlyEqual(expected: SkValue, actual: SkValue, crossinline message: ()->String = { "Expected: $expected (${expected.getKind()})\n  Actual: $actual (${actual.getKind()})" }) {
    assertTrue(strictlyEqual(expected, actual)) { message() }
}