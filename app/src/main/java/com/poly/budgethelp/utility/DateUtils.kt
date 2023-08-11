package com.poly.budgethelp.utility

import android.util.Log
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateUtils {
    companion object {
        fun longToDateString(value: Long): String {
            val date = Date(value)
            val pattern = "dd.MM.yyyy"
            val format = SimpleDateFormat(pattern)
            return format.format(date)
        }

        fun dateToLong(value: String) {

        }

        fun getFirstDayOfMonth(time: Long): Long {
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = time
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            return calendar.timeInMillis
            // val date: Date = Date(time)
        }

        fun getFirstDayOfWeek(time: Long): Long {
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = time
            calendar.set(Calendar.DAY_OF_WEEK, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            return calendar.timeInMillis
        }
    }
}