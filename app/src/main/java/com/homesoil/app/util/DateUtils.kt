package com.homesoil.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    fun parseIsoDateTime(dateString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateString, isoFormatter)
        } catch (e: DateTimeParseException) {
            try {
                // Try parsing without timezone info
                LocalDateTime.parse(dateString.substringBefore("+").substringBefore("Z"))
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun formatDisplayDate(dateString: String): String {
        val dateTime = parseIsoDateTime(dateString) ?: return dateString
        return dateTime.format(displayDateFormatter)
    }

    fun formatDisplayTime(dateString: String): String {
        val dateTime = parseIsoDateTime(dateString) ?: return dateString
        return dateTime.format(displayTimeFormatter)
    }

    fun formatDisplayDateTime(dateString: String): String {
        val dateTime = parseIsoDateTime(dateString) ?: return dateString
        return dateTime.format(displayDateTimeFormatter)
    }

    fun toIsoString(date: LocalDate): String {
        return date.atStartOfDay().format(isoFormatter)
    }

    fun today(): LocalDate = LocalDate.now()

    fun oneWeekAgo(): LocalDate = LocalDate.now().minusWeeks(1)

    fun oneMonthAgo(): LocalDate = LocalDate.now().minusMonths(1)
}
