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

        // TODO: This implementation should be checked for dates
        // What happens if we attempt to check 31st from a month that has 30 days?
        fun getDayLastMonth(time: Long): Long {
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = time
            calendar.add(Calendar.MONTH, -1)
            return calendar.timeInMillis
        }

        fun getDayLastHour(time: Long): Long {
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = time
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
            return calendar.timeInMillis
        }

        /**
         * Returns each full month start time between the given timeframe
         *
         */
        fun getStartOfMonthsBetween(start: Long, end: Long): ArrayList<Long> {
            val timeSteps: ArrayList<Long> = arrayListOf()
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = start
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            timeSteps.add(start)

            while (calendar.timeInMillis <= end) {
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1)
                timeSteps.add(calendar.timeInMillis)
            }

            return timeSteps
        }

        fun getStartOfWeeksBetween(start: Long, end: Long): ArrayList<Long> {
            val timeSteps: ArrayList<Long> = arrayListOf()
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = start

            val firstMonth = calendar.get(Calendar.MONTH)
            val firstDay = getFixedFirstDayOfWeek(calendar.timeInMillis)

            calendar.timeInMillis = firstDay

            // If month changes, override first time to start time
            if (calendar.get(Calendar.MONTH) != firstMonth) {
                timeSteps.add(start)
            } else {
                timeSteps.add(firstDay)
            }

            while (calendar.timeInMillis <= end) {
                // 7 day addition + 1 hour addition
                // Should always return Mondays, when setting HOUR_OF_DAY to 0
                calendar.timeInMillis += (86400000L * 7L + 3600000L)
                calendar.set(Calendar.HOUR_OF_DAY, 0)

                timeSteps.add(calendar.timeInMillis)
            }

            return timeSteps
        }

        // Return Monday hour 0 of the given week
        fun getFixedFirstDayOfWeek(time: Long): Long {
            val calendar: Calendar = Calendar.getInstance(Locale("en", "UK"))
            calendar.timeInMillis = time
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            while(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, -1)
            }

            return calendar.timeInMillis
        }
    }
}