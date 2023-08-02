package com.poly.budgethelp.utility

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date

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
    }
}