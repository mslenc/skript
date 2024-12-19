package skript.templates

import skript.io.SkriptEnv
import skript.io.toSkript
import skript.isInteger
import skript.util.SkArguments
import skript.values.*
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

internal val formatStyleNames = enumValues<FormatStyle>().map { listOf(it.name to it, it.name.lowercase() to it) }.flatten().toMap()

fun createPercentFormatter(locale: Locale, minDigits: Int, maxDigits: Int): NumberFormat {
    val formatter = NumberFormat.getPercentInstance(locale)
    formatter.minimumFractionDigits = minDigits
    formatter.maximumFractionDigits = maxDigits
    return formatter
}

class FormatPercentage(private val defaultFormat: NumberFormat, private val locale: Locale) : SkFunction("perc", listOf("value", "fallback", "minDigits", "maxDigits")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = (args.extractArg("value") as? SkNumber)?.toBigDecimal()
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""
        val minDigits = args.extractArg("minDigits").toNonNegativeIntOrNull()
        val maxDigits = args.extractArg("maxDigits").toNonNegativeIntOrNull()

        if (value == null)
            return fallbackStr.toSkript()

        if (minDigits == null && maxDigits == null || ((minDigits == null || minDigits == defaultFormat.minimumFractionDigits) && (maxDigits == null || maxDigits == defaultFormat.maximumFractionDigits)))
            return defaultFormat.format(value).toSkript()

        return createPercentFormatter(
            locale,
            minDigits = minDigits ?: defaultFormat.minimumFractionDigits,
            maxDigits = maxDigits ?: defaultFormat.maximumFractionDigits
        ).format(value).toSkript()
    }
}

class FormatInteger(private val defaultFormat: NumberFormat) : SkFunction("integer", listOf("value", "fallback")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = (args.extractArg("value") as? SkNumber)?.toBigDecimal()
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""

        if (value == null)
            return fallbackStr.toSkript()

        return defaultFormat.format(value).toSkript()
    }
}

fun createNumberFormatter(locale: Locale, minDigits: Int, maxDigits: Int): NumberFormat {
    val formatter = NumberFormat.getNumberInstance(locale)
    formatter.minimumFractionDigits = minDigits
    formatter.maximumFractionDigits = maxDigits
    return formatter
}

class FormatNumber(private val defaultFormat: NumberFormat, private val locale: Locale) : SkFunction("number", listOf("value", "fallback", "minDigits", "maxDigits", "preferInt")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = (args.extractArg("value") as? SkNumber)?.toBigDecimal()
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""

        if (value == null)
            return fallbackStr.toSkript()

        val minDigitsDef = args.extractArg("minDigits").toNonNegativeIntOrNull()
        val maxDigitsDef = args.extractArg("maxDigits").toNonNegativeIntOrNull()
        val preferInt = args.extractArg("preferInt").asBoolean().value && value.isInteger()
        val minDigits = if (preferInt) 0 else minDigitsDef
        val maxDigits = if (preferInt) 0 else maxDigitsDef

        if (minDigits == null && maxDigits == null || ((minDigits == null || minDigits == defaultFormat.minimumFractionDigits) && (maxDigits == null || maxDigits == defaultFormat.maximumFractionDigits)))
            return defaultFormat.format(value).toSkript()

        return createNumberFormatter(
            locale,
            minDigits = minDigits ?: defaultFormat.minimumFractionDigits,
            maxDigits = maxDigits ?: defaultFormat.maximumFractionDigits,
        ).format(value).toSkript()
    }
}

fun createMoneyFormatter(locale: Locale, currency: Currency, minDigits: Int, maxDigits: Int): NumberFormat {
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = currency
    formatter.minimumFractionDigits = minDigits
    formatter.maximumFractionDigits = maxDigits
    return formatter
}

class FormatMoney(private val defaultFormat: NumberFormat, private val locale: Locale, private val currency: Currency) : SkFunction("money", listOf("value", "fallback", "minDigits", "maxDigits")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = (args.extractArg("value") as? SkNumber)?.toBigDecimal()
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""
        val minDigits = args.extractArg("minDigits").toNonNegativeIntOrNull()
        val maxDigits = args.extractArg("maxDigits").toNonNegativeIntOrNull()

        if (value == null)
            return fallbackStr.toSkript()

        if (minDigits == null && maxDigits == null || ((minDigits == null || minDigits == defaultFormat.minimumFractionDigits) && (maxDigits == null || maxDigits == defaultFormat.maximumFractionDigits)))
            return defaultFormat.format(value).toSkript()

        return createMoneyFormatter(
            locale, currency,
            minDigits = minDigits ?: defaultFormat.minimumFractionDigits,
            maxDigits = maxDigits ?: defaultFormat.maximumFractionDigits
        ).format(value).toSkript()
    }
}

class FormatDate(private val defaultFormat: DateTimeFormatter, private val locale: Locale, private val timeZone: ZoneId = ZoneId.systemDefault()) : SkFunction("date", listOf("value", "format", "fallback")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = args.extractArg("value")
        val formatStr = args.extractArg("format")
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""

        val date = when (val d = value.unwrap()) { // unlike when displaying time, the default formats don't include the time zone, so we don't try to ensure the zone is there
            null -> null
            is LocalDate -> d
            is LocalDateTime -> d
            is Instant -> d.atZone(timeZone)
            is ZonedDateTime -> d
            is OffsetDateTime -> d
            else -> null
        } ?: return fallbackStr.toSkript()

        val formatter = formatStr.asStringOrNull()?.let { str ->
            val style = formatStyleNames[str]
            if (style != null) {
                DateTimeFormatter.ofLocalizedDate(style).withLocale(locale)
            } else {
                try {
                    DateTimeFormatter.ofPattern(str, locale)
                } catch (e: Exception) {
                    null
                }
            }
        } ?: defaultFormat

        return formatter.format(date).toSkript()
    }
}

class FormatDateTime(private val defaultFormat: DateTimeFormatter, private val locale: Locale, private val zone: ZoneId = ZoneId.systemDefault()) : SkFunction("dateTime", listOf("value", "format", "fallback")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = args.extractArg("value")
        val formatStr = args.extractArg("format")
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""

        val date = when (val d = value.unwrap()) {
            null -> null
            is LocalDateTime -> d.atZone(zone)
            is LocalDate -> d.atTime(12, 0).atZone(zone)
            is Instant -> ZonedDateTime.ofInstant(d, zone)
            is ZonedDateTime -> d
            is OffsetDateTime -> d.atZoneSameInstant(zone)
            else -> null
        } ?: return fallbackStr.toSkript()

        val formatter = formatStr.asStringOrNull()?.let { str ->
            val style = formatStyleNames[str]
            if (style != null) {
                DateTimeFormatter.ofLocalizedDateTime(style).withLocale(locale)
            } else {
                try {
                    DateTimeFormatter.ofPattern(str, locale)
                } catch (e: Exception) {
                    null
                }
            }
        } ?: defaultFormat

        return formatter.format(date).toSkript()
    }
}

class FormatTime(private val defaultFormat: DateTimeFormatter, private val locale: Locale, private val zone: ZoneId = ZoneId.systemDefault()) : SkFunction("time", listOf("value", "format", "fallback")) {
    override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
        val value = args.extractArg("value")
        val formatStr = args.extractArg("format")
        val fallbackStr = args.extractArg("fallback").asStringOrNull(true) ?: ""

        val dateTime = when (val d = value.unwrap()) {
            null -> null
            is LocalTime -> ZonedDateTime.of(LocalDate.now(zone), d, zone)
            is LocalDateTime -> d.atZone(zone)
            is Instant -> ZonedDateTime.ofInstant(d, zone)
            is ZonedDateTime -> d
            is OffsetDateTime -> d.atZoneSameInstant(zone)
            else -> null
        } ?: return fallbackStr.toSkript()

        val formatter = formatStr.asStringOrNull()?.let { str ->
            val style = formatStyleNames[str]
            if (style != null) {
                DateTimeFormatter.ofLocalizedTime(style).withLocale(locale)
            } else {
                try {
                    DateTimeFormatter.ofPattern(str, locale)
                } catch (e: Exception) {
                    null
                }
            }
        } ?: defaultFormat

        return formatter.format(dateTime).toSkript()
    }
}

private fun SkValue.asStringOrNull(allowEmpty: Boolean = true): String? {
    val str = when {
        this is SkNull -> return null
        this is SkUndefined -> return null
        else -> this.asString().value
    }

    return when {
        str.isNotEmpty() -> str
        allowEmpty -> str
        else -> null
    }
}