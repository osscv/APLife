package net.dkly.aplife.ui

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val DAY_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
internal val LONG_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
internal val TIME_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

internal fun formatDayDate(iso: String): String =
    runCatching { OffsetDateTime.parse(iso).format(DAY_DATE_FMT) }.getOrDefault(iso)

internal fun formatTimeRange(fromIso: String, toIso: String): String {
    val from = runCatching { OffsetDateTime.parse(fromIso).format(TIME_FMT) }.getOrDefault(fromIso)
    val to = runCatching { OffsetDateTime.parse(toIso).format(TIME_FMT) }.getOrDefault(toIso)
    return "$from – $to"
}

internal fun formatEpoch(ms: Long?): String {
    if (ms == null || ms <= 0) return "—"
    val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return zdt.format(LONG_DATE_FMT) + " · " + zdt.format(TIME_FMT)
}

internal fun formatDate(date: LocalDate): String = date.format(LONG_DATE_FMT)
