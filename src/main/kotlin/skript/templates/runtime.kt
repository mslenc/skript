package skript.templates

import skript.values.SkFunction
import skript.values.SkMap
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.HashMap

class TemplateRuntime(private val receiver: Appendable, val filters: SkMap, val escapes: SkMap, defaultEscapeKey: String) {
    val defaultEscape = escapes.entries[defaultEscapeKey] ?: throw IllegalArgumentException("Unknown escape key: $defaultEscapeKey")

    fun emit(text: String) {
        receiver.append(text)
    }

    companion object {
        val DEFAULT_ESCAPES = listOf(EscapeRaw, EscapeHtml, EscapeJs, EscapeUrl, EscapeMarkdown).associateBy { it.name }

        fun createDefaultFilters(locale: Locale = Locale.US, timeZone: ZoneId = ZoneId.systemDefault(), currency: Currency = Currency.getInstance(locale)): MutableMap<String, SkFunction> {
            val res = HashMap<String, SkFunction>()

            val percDefault = createPercentFormatter(locale, 0, 1)
            res["perc"] = FormatPercentage(percDefault, locale)

            val intDefault = NumberFormat.getIntegerInstance(locale)
            res["integer"] = FormatInteger(intDefault)

            val numDefault = createNumberFormatter(locale, 0, 3)
            res["number"] = FormatNumber(numDefault, locale)

            val moneyDefault = createMoneyFormatter(locale, currency, 2, 2)
            res["money"] = FormatMoney(moneyDefault, locale, currency)

            val dateDefault = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            res["date"] = FormatDate(dateDefault, locale, timeZone)

            val dateTimeDefault = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale)
            res["dateTime"] = FormatDateTime(dateTimeDefault, locale, timeZone)

            val timeDefault = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            res["time"] = FormatTime(timeDefault, locale, timeZone)

            return res
        }

        fun createWithDefaults(
            receiver: Appendable,
            defaultEscapeKey: String = "raw",
            locale: Locale = Locale.US,
            timeZone: ZoneId = ZoneId.systemDefault(),
            currency: Currency = Currency.getInstance(locale),
        ): TemplateRuntime {
            val filters = createDefaultFilters(locale, timeZone, currency)
            val escapes = DEFAULT_ESCAPES
            return TemplateRuntime(receiver, SkMap(filters), SkMap(escapes), defaultEscapeKey)
        }
    }
}

interface TemplateInstance {
    suspend fun execute(ctx: SkMap, out: TemplateRuntime)
}