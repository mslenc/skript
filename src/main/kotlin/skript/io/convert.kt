package skript.io

import skript.values.SkBoolean
import skript.values.SkNull
import skript.values.SkNumber
import skript.values.SkString
import java.math.BigDecimal
import java.math.BigInteger

fun String.toSkript() = SkString(this)
fun String?.toSkript() = this?.toSkript() ?: SkNull

fun Char.toSkript() = SkString(this.toString())
fun Char?.toSkript() = this?.toSkript() ?: SkNull

fun CharSequence.toSkript() = SkString(this.toString())
fun CharSequence?.toSkript() = this?.toSkript() ?: SkNull

fun Boolean.toSkript() = if (this) SkBoolean.TRUE else SkBoolean.FALSE
fun Boolean?.toSkript() = this?.toSkript() ?: SkNull

fun BigDecimal.toSkript() = SkNumber.valueOf(this)
fun BigDecimal?.toSkript() = this?.toSkript() ?: SkNull

fun BigInteger.toSkript() = SkNumber.valueOf(this)
fun BigInteger?.toSkript() = this?.toSkript() ?: SkNull

fun Double.toSkript() = SkNumber.valueOf(this)
fun Double?.toSkript() = this?.toSkript() ?: SkNull

fun Float.toSkript() = SkNumber.valueOf(this.toDouble())
fun Float?.toSkript() = this?.toSkript() ?: SkNull

fun Long.toSkript() = SkNumber.valueOf(this)
fun Long?.toSkript() = this?.toSkript() ?: SkNull

fun Int.toSkript() = SkNumber.valueOf(this)
fun Int?.toSkript() = this?.toSkript() ?: SkNull

fun Short.toSkript() = SkNumber.valueOf(this.toInt())
fun Short?.toSkript() = this?.toSkript() ?: SkNull

fun Byte.toSkript() = SkNumber.valueOf(this.toInt())
fun Byte?.toSkript() = this?.toSkript() ?: SkNull

fun Number.toSkript() = when(this) {
    is Int -> this.toSkript()
    is Double -> this.toSkript()
    is BigDecimal -> this.toSkript()
    is Long -> this.toSkript()
    is Float -> this.toSkript()
    is BigInteger -> this.toSkript()
    is Short -> this.toSkript()
    is Byte -> this.toSkript()
    else -> this.toDouble().toSkript()
}
fun Number?.toSkript() = this?.toSkript() ?: SkNull