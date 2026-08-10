package com.example.householdapp.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy \u2022 h:mm a", Locale.getDefault())

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

fun formatTimestamp(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val zoned = if (iso.endsWith("Z")) {
            Instant.parse(iso).atZone(ZoneId.systemDefault())
        } else {
            ZonedDateTime.parse(iso).withZoneSameInstant(ZoneId.systemDefault())
        }
        zoned.format(timestampFormatter)
    } catch (_: Exception) {
        iso
    }
}

fun formatDateOnly(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val date = if (iso.length == 10) {
            LocalDate.parse(iso)
        } else {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        date.format(dateFormatter)
    } catch (_: Exception) {
        iso
    }
}
